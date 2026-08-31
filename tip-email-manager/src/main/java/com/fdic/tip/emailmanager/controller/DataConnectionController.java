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
import gov.fdic.tip.emailmanager.dto.DataConnectionDto;
import gov.fdic.tip.emailmanager.dto.TestConnectionRequest;
import gov.fdic.tip.emailmanager.enums.ActorType;
import gov.fdic.tip.emailmanager.service.BusinessAuditService;
import gov.fdic.tip.emailmanager.service.DataConnectionService;
import gov.fdic.tip.emailmanager.util.LoggerHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/data-connections")
@RequiredArgsConstructor
public class DataConnectionController {

    private final DataConnectionService dataConnectionService;
    private final BusinessAuditService businessAuditService;
    private final LoggerHelper loggerHelper;

    @GetMapping
    @PreAuthorize(AppConstants.PERM_DATA_CONNECTION_VIEW)
    public ResponseEntity<List<DataConnectionDto>> getAllConnections(Authentication authentication) {
        // method body...
    }

    @PostMapping
    @PreAuthorize(AppConstants.PERM_DATA_CONNECTION_ADD)
    public ResponseEntity<DataConnectionDto> createConnection(
            @Valid @RequestBody DataConnectionDto dto,
            Authentication authentication) {
        // method body...
    }

    @PutMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_DATA_CONNECTION_EDIT)
    public ResponseEntity<DataConnectionDto> updateConnection(
            @PathVariable Long id,
            @Valid @RequestBody DataConnectionDto dto,
            Authentication authentication) {
        // method body...
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_DATA_CONNECTION_DELETE)
    public ResponseEntity<Void> deleteConnection(
            @PathVariable Long id,
            Authentication authentication) {
        // method body...
    }

    @PostMapping("/test")
    @PreAuthorize(AppConstants.PERM_DATA_CONNECTION_TEST)
    public ResponseEntity<Boolean> testConnection(
            @Valid @RequestBody TestConnectionRequest request,
            Authentication authentication) {
        // method body...
    }
    @GetMapping
    public ResponseEntity<List<DataConnectionDto>> getAllConnections(Authentication authentication) {
        String actorEmail = resolveUserEmail(authentication);

        try {
            List<DataConnectionDto> connections = dataConnectionService.getAllConnections();

            emitAuditEvent(
                "DATA_CONNECTION_READ_ALL",
                "ALL",
                "Data Connections List",
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("count", connections.size()),
                "SUCCESS"
            );

            return ResponseEntity.ok(connections);
        } catch (Exception e) {
            emitAuditEvent(
                "DATA_CONNECTION_READ_ALL",
                "ALL",
                "Data Connections List",
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
    public ResponseEntity<DataConnectionDto> createConnection(
            @Valid @RequestBody DataConnectionDto dto,
            Authentication authentication) {

        String actorEmail = resolveUserEmail(authentication);

        try {
            DataConnectionDto created = dataConnectionService.createConnection(dto, actorEmail);

            emitAuditEvent(
                "DATA_CONNECTION_CREATE",
                String.valueOf(created.getId()),
                created.getName(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("connectionName", created.getName(), "status", created.getStatus()),
                "SUCCESS"
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            emitAuditEvent(
                "DATA_CONNECTION_CREATE",
                "UNKNOWN",
                dto.getName(),
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
    public ResponseEntity<DataConnectionDto> updateConnection(
            @PathVariable Long id,
            @Valid @RequestBody DataConnectionDto dto,
            Authentication authentication) {

        String actorEmail = resolveUserEmail(authentication);

        try {
            DataConnectionDto updated = dataConnectionService.updateConnection(id, dto, actorEmail);

            emitAuditEvent(
                "DATA_CONNECTION_UPDATE",
                String.valueOf(id),
                updated.getName(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("connectionName", updated.getName(), "status", updated.getStatus()),
                "SUCCESS"
            );

            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            emitAuditEvent(
                "DATA_CONNECTION_UPDATE",
                String.valueOf(id),
                dto.getName(),
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
    public ResponseEntity<Void> deleteConnection(
            @PathVariable Long id,
            Authentication authentication) {

        String actorEmail = resolveUserEmail(authentication);

        try {
            dataConnectionService.deleteConnection(id, actorEmail);

            emitAuditEvent(
                "DATA_CONNECTION_DELETE",
                String.valueOf(id),
                "Data Connection " + id,
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("action", "DELETE"),
                "SUCCESS"
            );

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            emitAuditEvent(
                "DATA_CONNECTION_DELETE",
                String.valueOf(id),
                "Data Connection " + id,
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("error", e.getMessage()),
                "FAILURE"
            );
            throw e;
        }
    }

    @PostMapping("/test")
    public ResponseEntity<Boolean> testConnection(
            @Valid @RequestBody TestConnectionRequest request,
            Authentication authentication) {

        String actorEmail = resolveUserEmail(authentication);

        try {
            boolean isSuccessful = dataConnectionService.testConnection(request);

            emitAuditEvent(
                "DATA_CONNECTION_TEST",
                String.valueOf(request.getDatabaseLocationId()),
                "Test Connection Request",
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("databaseLocationId", request.getDatabaseLocationId(), "testResult", isSuccessful),
                isSuccessful ? "SUCCESS" : "FAILURE"
            );

            return ResponseEntity.ok(isSuccessful);
        } catch (Exception e) {
            emitAuditEvent(
                "DATA_CONNECTION_TEST",
                String.valueOf(request.getDatabaseLocationId()),
                "Test Connection Request",
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
                    .module("DATA_CONNECTION")
                    .targetEntityType("DataConnection")
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