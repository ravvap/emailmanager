package gov.fdic.tip.emailmanager.service.impl;

import gov.fdic.tip.emailmanager.constants.AttributeType;
import gov.fdic.tip.emailmanager.constants.ContactStatus;
import gov.fdic.tip.emailmanager.dto.ContactAttributeDto;
import gov.fdic.tip.emailmanager.entity.ContactAttribute;
import gov.fdic.tip.emailmanager.entity.ContactAttributeOption;
import gov.fdic.tip.emailmanager.repository.ContactAttributeRepository;
import gov.fdic.tip.emailmanager.service.ContactAttributeService;
import gov.fdic.tip.emailmanager.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ContactAttributeServiceImpl implements ContactAttributeService {

    private final ContactAttributeRepository repository;
    private final AuditLogService auditLogService;

    public ContactAttributeServiceImpl(ContactAttributeRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactAttributeDto> getAllAttributes() {
        return repository.findByDeletedAtIsNull().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactAttributeDto> getActiveAttributes() {
        return repository.findByStatusAndDeletedAtIsNull(ContactStatus.ACTIVE).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ContactAttributeDto createAttribute(ContactAttributeDto dto, String username) {
        // Validation 1: Unique name check
        if (repository.existsByNameIgnoreCaseAndDeletedAtIsNull(dto.getName())) {
            throw new IllegalArgumentException("Attribute with name '" + dto.getName() + "' already exists.");
        }

        // Validation 2: Fixed List requires options
        if (AttributeType.FIXED_LIST.equalsIgnoreCase(dto.getType()) && (dto.getOptions() == null || dto.getOptions().isEmpty())) {
            throw new IllegalArgumentException("Fixed List type requires at least one default option value.");
        }

        ContactAttribute entity = new ContactAttribute();
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setDefaultValue(dto.getDefaultValue());
        // Default to ACTIVE when the client omits status — the DB column
        // has DEFAULT 'ACTIVE' too, but passing an explicit NULL here
        // overrides that default and trips the NOT NULL constraint.
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : ContactStatus.ACTIVE);
        entity.setCreatedBy(username);

        if (AttributeType.FIXED_LIST.equalsIgnoreCase(dto.getType()) && dto.getOptions() != null) {
            for (String val : dto.getOptions()) {
                ContactAttributeOption opt = new ContactAttributeOption();
                opt.setAttribute(entity);
                opt.setOptionValue(val);
                opt.setCreatedBy(username);
                entity.getOptions().add(opt);
            }
        }

        ContactAttribute saved = repository.save(entity);

        auditLogService.logAction(
                "CONTACT_ATTRIBUTE",
                saved.getId(),
                "CREATE",
                username,
                "Created attribute: " + saved.getName() + " with type: " + saved.getType()
        );

        return convertToDto(saved);
    }

    @Override
    public ContactAttributeDto updateAttribute(Long id, ContactAttributeDto dto, String username) {
        ContactAttribute entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Contact attribute not found with id: " + id));

        // Validation 1: Duplicate name check for other records
        if (repository.existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(dto.getName(), id)) {
            throw new IllegalArgumentException("Another attribute with name '" + dto.getName() + "' already exists.");
        }

        // Validation 2: Rule - Type cannot be changed upon edit
        if (!entity.getType().equalsIgnoreCase(dto.getType())) {
            throw new IllegalArgumentException("Attribute type is not editable once created.");
        }

        // Validation 3: Rule - Default values currently in use by contacts cannot be removed
        if (AttributeType.FIXED_LIST.equalsIgnoreCase(entity.getType())) {
            List<String> existingOptions = entity.getOptions().stream()
                    .map(ContactAttributeOption::getOptionValue)
                    .collect(Collectors.toList());

            List<String> incomingOptions = dto.getOptions() != null ? dto.getOptions() : new ArrayList<>();

            for (String existingOpt : existingOptions) {
                if (!incomingOptions.contains(existingOpt)) {
                    // Check if a removed option is still assigned to existing contacts.
                    boolean inUse = repository.isOptionInUseByContacts(id, existingOpt);
                    if (inUse) {
                        throw new IllegalArgumentException("Cannot remove option '" + existingOpt + "' because it is currently assigned to existing contacts.");
                    }
                }
            }

            syncOptions(entity, incomingOptions, username);
        } else {
            entity.setDefaultValue(dto.getDefaultValue());
        }

        entity.setName(dto.getName());
        entity.setStatus(dto.getStatus());
        entity.setUpdatedBy(username);

        ContactAttribute updated = repository.save(entity);

        auditLogService.logAction(
                "CONTACT_ATTRIBUTE",
                updated.getId(),
                "UPDATE",
                username,
                "Updated attribute: " + updated.getName()
        );

        return convertToDto(updated);
    }

    @Override
    public void deleteAttribute(Long id, String username) {
        ContactAttribute entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Contact attribute not found with id: " + id));

        entity.setDeletedBy(username);
        entity.setDeletedAt(OffsetDateTime.now());
        repository.save(entity);

        auditLogService.logAction(
                "CONTACT_ATTRIBUTE",
                id,
                "DELETE",
                username,
                "Deleted contact attribute: " + entity.getName()
        );
    }

    /**
     * Reconciles an attribute's option list against the incoming values
     * instead of clear()-then-rebuild, so options that are unchanged
     * keep their original createdBy/createdAt, and only additions and
     * removals actually touch the database.
     *
     * Assumes the attribute -> options mapping is configured with
     * orphanRemoval = true; if it isn't, the removal branch needs an
     * explicit repository delete instead of relying on collection
     * removal to cascade.
     */
    private void syncOptions(ContactAttribute entity, List<String> incomingOptions, String username) {
        Map<String, ContactAttributeOption> existingByValue = entity.getOptions().stream()
                .collect(Collectors.toMap(
                        opt -> opt.getOptionValue().toLowerCase(),
                        opt -> opt,
                        (a, b) -> a));

        entity.getOptions().removeIf(opt ->
                incomingOptions.stream().noneMatch(v -> v.equalsIgnoreCase(opt.getOptionValue())));

        for (String val : incomingOptions) {
            if (!existingByValue.containsKey(val.toLowerCase())) {
                ContactAttributeOption opt = new ContactAttributeOption();
                opt.setAttribute(entity);
                opt.setOptionValue(val);
                opt.setCreatedBy(username);
                entity.getOptions().add(opt);
            }
        }
    }

    private ContactAttributeDto convertToDto(ContactAttribute entity) {
        ContactAttributeDto dto = new ContactAttributeDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setDefaultValue(entity.getDefaultValue());
        dto.setStatus(entity.getStatus());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getOptions() != null) {
            dto.setOptions(entity.getOptions().stream()
                    .map(ContactAttributeOption::getOptionValue)
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}