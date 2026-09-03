package gov.fdic.tip.emailmanager.service.impl;

import gov.fdic.tip.emailmanager.constants.AttributeType;
import gov.fdic.tip.emailmanager.constants.ContactStatus;
import gov.fdic.tip.emailmanager.dto.ContactAttributeValueDto;
import gov.fdic.tip.emailmanager.dto.ContactDto;
import gov.fdic.tip.emailmanager.entity.Contact;
import gov.fdic.tip.emailmanager.entity.ContactAttribute;
import gov.fdic.tip.emailmanager.entity.ContactAttributeValue;
import gov.fdic.tip.emailmanager.repository.ContactAttributeRepository;
import gov.fdic.tip.emailmanager.repository.ContactRepository;
import gov.fdic.tip.emailmanager.service.AuditLogService;
import gov.fdic.tip.emailmanager.service.ContactService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class ContactServiceImpl implements ContactService {

    // RFC-5322-lite: good enough to reject obvious garbage without
    // rejecting legitimate addresses. Matches the level of strictness
    // already used for ApprovedSender's domain check.
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final ContactRepository contactRepository;
    private final ContactAttributeRepository attributeRepository;
    private final AuditLogService auditLogService;

    public ContactServiceImpl(ContactRepository contactRepository,
                              ContactAttributeRepository attributeRepository,
                              AuditLogService auditLogService) {
        this.contactRepository = contactRepository;
        this.attributeRepository = attributeRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactDto> getAllContacts() {
        return contactRepository.findByDeletedAtIsNull().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactDto> searchContactsByAttribute(Long attributeId, String value) {
        return contactRepository.findByAttributeAndValue(attributeId, value).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ContactDto createContact(ContactDto dto, String username) {
        String normalizedEmail = validateAndNormalizeEmail(dto.getEmail());

        // Rule: Duplicate active email validation
        if (ContactStatus.ACTIVE.equalsIgnoreCase(dto.getStatus())
                && contactRepository.existsActiveEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Contact email already exists.");
        }

        Contact contact = new Contact();
        contact.setName(dto.getName());
        contact.setEmail(normalizedEmail);
        contact.setOrganization(dto.getOrganization());
        contact.setStatus(dto.getStatus());
        contact.setCreatedBy(username);

        // Process optional dynamic metadata attributes
        syncAttributeValues(dto, contact, username);

        Contact saved = contactRepository.save(contact);

        auditLogService.logAction("CONTACT", saved.getId(), "CREATE", username,
                "Created contact: " + saved.getEmail());

        return convertToDto(saved);
    }

    @Override
    public ContactDto updateContact(Long id, ContactDto dto, String username) {
        Contact contact = contactRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Contact not found with id: " + id));

        String normalizedEmail = validateAndNormalizeEmail(dto.getEmail());

        // Rule: Prevent setting to active if another active contact has this email
        if (ContactStatus.ACTIVE.equalsIgnoreCase(dto.getStatus()) &&
                contactRepository.existsActiveEmailExcludingId(normalizedEmail, id)) {
            throw new IllegalArgumentException("Contact email already exists.");
        }

        contact.setName(dto.getName());
        contact.setEmail(normalizedEmail);
        contact.setOrganization(dto.getOrganization());
        contact.setStatus(dto.getStatus());
        contact.setUpdatedBy(username);

        syncAttributeValues(dto, contact, username);

        Contact updated = contactRepository.save(contact);

        auditLogService.logAction("CONTACT", updated.getId(), "UPDATE", username,
                "Updated contact status to " + updated.getStatus());

        return convertToDto(updated);
    }

    @Override
    public void deleteContact(Long id, String username) {
        Contact contact = contactRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Contact not found with id: " + id));

        long listCount = contactRepository.countListMemberships(id);
        long sendCount = contactRepository.countSendHistory(id);

        // Deletion rule:
        // - Permanent deletion if never referenced in list membership/send history.
        // - Deactivated (soft-deleted) if referenced, preserving lists and history.
        if (listCount == 0 && sendCount == 0) {
            contactRepository.delete(contact);
            auditLogService.logAction("CONTACT", id, "HARD_DELETE", username,
                    "Permanently deleted unreferenced contact.");
        } else {
            contact.setStatus(ContactStatus.INACTIVE);
            contact.setDeletedBy(username);
            contact.setDeletedAt(OffsetDateTime.now());
            contactRepository.save(contact);

            auditLogService.logAction("CONTACT", id, "SOFT_DELETE", username,
                    "Soft-deleted referenced contact (deactivated).");
        }
    }

    private String validateAndNormalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        String normalized = email.toLowerCase().trim();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Email address '" + email + "' is not a valid format.");
        }
        return normalized;
    }

    /**
     * Validates each incoming attribute value against its registered
     * attribute type (EM-5 AC: "values are validated against the
     * registered type"), then reconciles them onto the contact.
     *
     * Used for both create (existing values map is empty) and update.
     * Unlike a clear()-then-rebuild, this preserves createdBy/createdAt
     * on values that are unchanged, only touching rows that were
     * actually added, edited, or removed.
     *
     * Assumes the contact -> attributeValues mapping is configured with
     * orphanRemoval = true; if it isn't, the removal branch below needs
     * an explicit attributeValueRepository.delete(...) call instead of
     * relying on collection removal to cascade.
     */
    private void syncAttributeValues(ContactDto dto, Contact contact, String username) {
        Map<Long, ContactAttributeValue> existingByAttributeId = contact.getAttributeValues().stream()
                .collect(Collectors.toMap(v -> v.getAttribute().getId(), v -> v));

        List<ContactAttributeValueDto> incoming = dto.getAttributeValues() != null
                ? dto.getAttributeValues()
                : List.of();

        Map<Long, ContactAttributeValueDto> incomingByAttributeId = incoming.stream()
                .collect(Collectors.toMap(ContactAttributeValueDto::getAttributeId, v -> v, (a, b) -> a));

        // Remove values for attributes no longer present in the incoming set.
        contact.getAttributeValues().removeIf(v -> !incomingByAttributeId.containsKey(v.getAttribute().getId()));

        for (ContactAttributeValueDto attrDto : incoming) {
            ContactAttribute attr = attributeRepository.findByIdAndDeletedAtIsNull(attrDto.getAttributeId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid attribute ID: " + attrDto.getAttributeId()));

            validateValueAgainstType(attr, attrDto.getValue());

            ContactAttributeValue existing = existingByAttributeId.get(attrDto.getAttributeId());
            if (existing != null) {
                if (!existing.getAttributeValue().equals(attrDto.getValue())) {
                    existing.setAttributeValue(attrDto.getValue());
                    existing.setUpdatedBy(username);
                    existing.setUpdatedAt(OffsetDateTime.now());
                }
            } else {
                ContactAttributeValue valEntity = new ContactAttributeValue();
                valEntity.setContact(contact);
                valEntity.setAttribute(attr);
                valEntity.setAttributeValue(attrDto.getValue());
                valEntity.setCreatedBy(username);
                contact.getAttributeValues().add(valEntity);
            }
        }
    }

    private void validateValueAgainstType(ContactAttribute attr, String value) {
        switch (attr.getType()) {
            case AttributeType.FIXED_LIST -> {
                boolean isValidOption = attr.getOptions().stream()
                        .anyMatch(opt -> opt.getOptionValue().equalsIgnoreCase(value));
                if (!isValidOption) {
                    throw new IllegalArgumentException(
                            "A free-form value like '" + value + "' is rejected for attribute '" + attr.getName() + "'.");
                }
            }
            case AttributeType.NUMBER -> {
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException | NullPointerException e) {
                    throw new IllegalArgumentException(
                            "Value '" + value + "' is not a valid number for attribute '" + attr.getName() + "'.");
                }
            }
            case AttributeType.DATE -> {
                try {
                    LocalDate.parse(value);
                } catch (DateTimeParseException | NullPointerException e) {
                    throw new IllegalArgumentException(
                            "Value '" + value + "' is not a valid date (expected ISO-8601, e.g. 2026-09-02) for attribute '" + attr.getName() + "'.");
                }
            }
            case AttributeType.TEXT -> {
                if (value == null || value.isBlank()) {
                    throw new IllegalArgumentException("Value for attribute '" + attr.getName() + "' cannot be blank.");
                }
            }
            default -> throw new IllegalStateException("Unknown attribute type: " + attr.getType());
        }
    }

    private ContactDto convertToDto(Contact contact) {
        ContactDto dto = new ContactDto();
        dto.setId(contact.getId());
        dto.setName(contact.getName());
        dto.setEmail(contact.getEmail());
        dto.setOrganization(contact.getOrganization());
        dto.setStatus(contact.getStatus());
        dto.setCreatedBy(contact.getCreatedBy());
        dto.setCreatedAt(contact.getCreatedAt());
        dto.setUpdatedBy(contact.getUpdatedBy());
        dto.setUpdatedAt(contact.getUpdatedAt());

        if (contact.getAttributeValues() != null) {
            dto.setAttributeValues(contact.getAttributeValues().stream().map(av -> {
                ContactAttributeValueDto vDto = new ContactAttributeValueDto();
                vDto.setAttributeId(av.getAttribute().getId());
                vDto.setAttributeName(av.getAttribute().getName());
                vDto.setValue(av.getAttributeValue());
                return vDto;
            }).collect(Collectors.toList()));
        }

        // Fetch distribution lists for modal notices when deactivating/editing
        dto.setAffectedDistributionLists(contactRepository.findAssociatedDistributionListNames(contact.getId()));

        return dto;
    }
}