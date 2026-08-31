package gov.fdic.tip.emailmanager.controller;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import gov.fdic.tip.emailmanager.dto.BusinessAuditEvent;
import gov.fdic.tip.emailmanager.dto.InternalDomainDto;
import gov.fdic.tip.emailmanager.enums.ActorType;
import gov.fdic.tip.emailmanager.service.BusinessAuditService;
import gov.fdic.tip.emailmanager.service.InternalDomainService;
import gov.fdic.tip.emailmanager.util.LoggerHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal-domains")
@RequiredArgsConstructor
public class InternalDomainController {

    private final InternalDomainService internalDomainService;
    private final BusinessAuditService businessAuditService;
    private final LoggerHelper loggerHelper;

    @GetMapping
    @PreAuthorize(AppConstants.PERM_DOMAIN_VIEW)
    public ResponseEntity<List<InternalDomainDto>> getAllDomains(Authentication authentication) {
        // method body...
    }

    @PostMapping
    @PreAuthorize(AppConstants.PERM_DOMAIN_ADD)
    public ResponseEntity<InternalDomainDto> createDomain(
            @Valid @RequestBody InternalDomainDto dto,
            Authentication authentication) {
        // method body...
    }

    @PutMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_DOMAIN_EDIT)
    public ResponseEntity<InternalDomainDto> updateDomain(
            @PathVariable Long id,
            @Valid @RequestBody InternalDomainDto dto,
            Authentication authentication) {
        // method body...
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_DOMAIN_DELETE)
    public ResponseEntity<Void> deleteDomain(
            @PathVariable Long id,
            Authentication authentication) {
        // method body...
    }
    @GetMapping
    public ResponseEntity<List<InternalDomainDto>> getAllDomains(Authentication authentication) {
        String actorEmail = resolveUserEmail(authentication);

        try {
            List<InternalDomainDto> domains = internalDomainService.getAllDomains();

            emitAuditEvent(
                "INTERNAL_DOMAIN_READ_ALL",
                "ALL",
                "Internal Domains List",
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("count", domains.size()),
                "SUCCESS"
            );

            return ResponseEntity.ok(domains);
        } catch (Exception e) {
            emitAuditEvent(
                "INTERNAL_DOMAIN_READ_ALL",
                "ALL",
                "Internal Domains List",
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("error", e.getMessage()),
                "FAILURE"
            );
            throw e;
        }
    }

    @PostMapping
    public ResponseEntity<InternalDomainDto> createDomain(
            @Valid @RequestBody InternalDomainDto dto,
            Authentication authentication) {

        String actorEmail = resolveUserEmail(authentication);

        try {
            InternalDomainDto created = internalDomainService.createDomain(dto, actorEmail);

            emitAuditEvent(
                "INTERNAL_DOMAIN_CREATE",
                String.valueOf(created.getId()),
                created.getDomainName(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("domainName", created.getDomainName()),
                "SUCCESS"
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            emitAuditEvent(
                "INTERNAL_DOMAIN_CREATE",
                "UNKNOWN",
                dto.getDomainName(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("error", e.getMessage()),
                "FAILURE"
            );
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<InternalDomainDto> updateDomain(
            @PathVariable Long id,
            @Valid @RequestBody InternalDomainDto dto,
            Authentication authentication) {

        String actorEmail = resolveUserEmail(authentication);

        try {
            InternalDomainDto updated = internalDomainService.updateDomain(id, dto, actorEmail);

            emitAuditEvent(
                "INTERNAL_DOMAIN_UPDATE",
                String.valueOf(id),
                updated.getDomainName(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("domainName", updated.getDomainName()),
                "SUCCESS"
            );

            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            emitAuditEvent(
                "INTERNAL_DOMAIN_UPDATE",
                String.valueOf(id),
                dto.getDomainName(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("error", e.getMessage()),
                "FAILURE"
            );
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDomain(
            @PathVariable Long id,
            Authentication authentication) {

        String actorEmail = resolveUserEmail(authentication);

        try {
            internalDomainService.deleteDomain(id, actorEmail);

            emitAuditEvent(
                "INTERNAL_DOMAIN_DELETE",
                String.valueOf(id),
                "Internal Domain " + id,
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("action", "DELETE"),
                "SUCCESS"
            );

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            emitAuditEvent(
                "INTERNAL_DOMAIN_DELETE",
                String.valueOf(id),
                "Internal Domain " + id,
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("error", e.getMessage()),
                "FAILURE"
            );
            throw e;
        }
    }

    private void emitAuditEvent(String eventType,
                                String targetEntityId,
                                String targetEntityLabel,
                                ActorType actorType,
                                String actorId,
                                String actorLabel,
                                Map<String, Object> details,
                                String outcome) {
        try {
            businessAuditService.logEvent(BusinessAuditEvent.builder()
                    .eventType(eventType)
                    .module("INTERNAL_DOMAIN")
                    .targetEntityType("InternalDomain")
                    .targetEntityId(targetEntityId != null ? targetEntityId : "UNKNOWN")
                    .targetEntityLabel(targetEntityLabel)
                    .actorType(actorType)
                    .actorId(actorId)
                    .actorLabel(actorLabel)
                    .details(details)
                    .outcome(outcome)
                    .build());
        } catch (Exception e) {
            loggerHelper.getLogger().warn("Unable to persist business audit event {}: {}", eventType, e.getMessage());
        }
    }

    private String resolveUserEmail(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return "UNKNOWN";
        }
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (StringUtils.isNotBlank(preferredUsername)) {
            return preferredUsername;
        }
        String email = jwt.getClaimAsString("email");
        return StringUtils.isNotBlank(email) ? email : "UNKNOWN";
    }
}