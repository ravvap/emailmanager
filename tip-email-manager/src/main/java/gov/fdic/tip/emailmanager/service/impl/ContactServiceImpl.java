package gov.fdic.tip.emailmanager.service.impl;

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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ContactServiceImpl implements ContactService {

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
        // Rule: Duplicate active email validation
        if ("Active".equalsIgnoreCase(dto.getStatus()) && contactRepository.existsActiveEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Contact email already exists.");
        }

        Contact contact = new Contact();
        contact.setName(dto.getName());
        contact.setEmail(dto.getEmail().toLowerCase().trim());
        contact.setOrganization(dto.getOrganization());
        contact.setStatus(dto.getStatus());
        contact.setCreatedBy(username);

        // Process optional dynamic metadata attributes
        mapAttributeValues(dto, contact, username);

        Contact saved = contactRepository.save(contact);

        auditLogService.logAction("CONTACT", saved.getId(), "CREATE", username,
                "Created contact: " + saved.getEmail());

        return convertToDto(saved);
    }

    @Override
    public ContactDto updateContact(Long id, ContactDto dto, String username) {
        Contact contact = contactRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Contact not found with id: " + id));

        // Rule: Prevent setting to active if another active contact has this email
        if ("Active".equalsIgnoreCase(dto.getStatus()) &&
                contactRepository.existsActiveEmailExcludingId(dto.getEmail(), id)) {
            throw new IllegalArgumentException("Contact email already exists.");
        }

        contact.setName(dto.getName());
        contact.setEmail(dto.getEmail().toLowerCase().trim());
        contact.setOrganization(dto.getOrganization());
        contact.setStatus(dto.getStatus());
        contact.setUpdatedBy(username);

        contact.getAttributeValues().clear();
        mapAttributeValues(dto, contact, username);

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
            contact.setStatus("Inactive");
            contact.setDeletedBy(username);
            contact.setDeletedAt(OffsetDateTime.now());
            contactRepository.save(contact);

            auditLogService.logAction("CONTACT", id, "SOFT_DELETE", username,
                    "Soft-deleted referenced contact (deactivated).");
        }
    }

    private void mapAttributeValues(ContactDto dto, Contact contact, String username) {
        if (dto.getAttributeValues() != null) {
            for (ContactAttributeValueDto attrDto : dto.getAttributeValues()) {
                ContactAttribute attr = attributeRepository.findByIdAndDeletedAtIsNull(attrDto.getAttributeId())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid attribute ID: " + attrDto.getAttributeId()));

                // Type validation for fixed list choices
                if ("Fixed List".equalsIgnoreCase(attr.getType())) {
                    boolean isValidOption = attr.getOptions().stream()
                            .anyMatch(opt -> opt.getOptionValue().equalsIgnoreCase(attrDto.getValue()));
                    if (!isValidOption) {
                        throw new IllegalArgumentException("A free-form value like '" + attrDto.getValue() + "' is rejected.");
                    }
                }

                ContactAttributeValue valEntity = new ContactAttributeValue();
                valEntity.setContact(contact);
                valEntity.setAttribute(attr);
                valEntity.setAttributeValue(attrDto.getValue());
                valEntity.setCreatedBy(username);
                contact.getAttributeValues().add(valEntity);
            }
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