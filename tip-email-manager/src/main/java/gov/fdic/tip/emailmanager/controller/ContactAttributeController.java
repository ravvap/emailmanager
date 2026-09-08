package gov.fdic.tip.emailmanager.controller;

import gov.fdic.tip.emailmanager.constant.AppConstants;
import gov.fdic.tip.emailmanager.constant.ActorType;
import gov.fdic.tip.emailmanager.dto.ContactAttributeDto;
import gov.fdic.tip.emailmanager.service.AuditLogService;
import gov.fdic.tip.emailmanager.service.ContactAttributeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/contact-attributes")
public class ContactAttributeController {

    private final ContactAttributeService contactAttributeService;
    private final AuditLogService auditLogService;

    public ContactAttributeController(ContactAttributeService contactAttributeService, AuditLogService auditLogService) {
        this.contactAttributeService = contactAttributeService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize(AppConstants.PERM_CONTACT_ATTR_VIEW)
    public ResponseEntity<List<ContactAttributeDto>> getAllContactAttributes(Authentication authentication) {
        String actorEmail = authentication.getName();
        log.info("Fetching all contact attributes by user: {}", actorEmail);

        List<ContactAttributeDto> attributes = contactAttributeService.getAllContactAttributes();

        auditLogService.emitAuditEvent(
                "CONTACT_ATTRIBUTE_READ_ALL",
                "ALL",
                "Contact Attributes List",
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("count", attributes.size()),
                "SUCCESS"
        );

        return ResponseEntity.ok(attributes);
    }

    @PostMapping
    @PreAuthorize(AppConstants.PERM_CONTACT_ATTR_ADD)
    public ResponseEntity<ContactAttributeDto> createContactAttribute(
            @Valid @RequestBody ContactAttributeDto dto,
            Authentication authentication) {
        String actorEmail = authentication.getName();
        log.info("Creating contact attribute: {} by user: {}", dto.getName(), actorEmail);

        ContactAttributeDto created = contactAttributeService.createContactAttribute(dto, actorEmail);

        auditLogService.emitAuditEvent(
                "CONTACT_ATTRIBUTE_CREATE",
                String.valueOf(created.getId()),
                created.getName(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("attributeName", created.getName()),
                "SUCCESS"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_CONTACT_ATTR_EDIT)
    public ResponseEntity<ContactAttributeDto> updateContactAttribute(
            @PathVariable Long id,
            @Valid @RequestBody ContactAttributeDto dto,
            Authentication authentication) {
        String actorEmail = authentication.getName();
        log.info("Updating contact attribute ID: {} by user: {}", id, actorEmail);

        ContactAttributeDto updated = contactAttributeService.updateContactAttribute(id, dto, actorEmail);

        auditLogService.emitAuditEvent(
                "CONTACT_ATTRIBUTE_UPDATE",
                String.valueOf(id),
                updated.getName(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("attributeName", updated.getName()),
                "SUCCESS"
        );

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_CONTACT_ATTR_DELETE)
    public ResponseEntity<Void> deleteContactAttribute(
            @PathVariable Long id,
            Authentication authentication) {
        String actorEmail = authentication.getName();
        log.info("Deleting contact attribute ID: {} by user: {}", id, actorEmail);

        contactAttributeService.deleteContactAttribute(id, actorEmail);

        auditLogService.emitAuditEvent(
                "CONTACT_ATTRIBUTE_DELETE",
                String.valueOf(id),
                "Contact Attribute " + id,
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("attributeId", id),
                "SUCCESS"
        );

        return ResponseEntity.noContent().build();
    }
}