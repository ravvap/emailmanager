package gov.fdic.tip.emailmanager.controller;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.fdic.tip.emailmanager.dto.BusinessAuditEvent;
import gov.fdic.tip.emailmanager.dto.NoReplyMailboxDto;
import gov.fdic.tip.emailmanager.enums.ActorType;
import gov.fdic.tip.emailmanager.service.BusinessAuditService;
import gov.fdic.tip.emailmanager.service.NoReplyMailboxService;
import gov.fdic.tip.emailmanager.util.LoggerHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/no-reply-mailbox")
@RequiredArgsConstructor
public class NoReplyMailboxController {

    private final NoReplyMailboxService mailboxService;
    private final BusinessAuditService businessAuditService;
    private final LoggerHelper loggerHelper;

    @GetMapping
    public ResponseEntity<NoReplyMailboxDto> getMailbox(Authentication authentication) {
        String actorEmail = resolveUserEmail(authentication);
        Map<String, Object> details = Map.of("action", "GET_NO_REPLY_MAILBOX");

        try {
            NoReplyMailboxDto result = mailboxService.getMailbox();

            emitAuditEvent(
                "NO_REPLY_MAILBOX_READ",
                String.valueOf(result.getId()),
                result.getEmailAddress(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                details,
                "SUCCESS"
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            emitAuditEvent(
                "NO_REPLY_MAILBOX_READ",
                "UNKNOWN",
                "No-Reply Mailbox",
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
    public ResponseEntity<NoReplyMailboxDto> saveOrUpdateMailbox(
            @Valid @RequestBody NoReplyMailboxDto dto,
            Authentication authentication) {

        String actorEmail = resolveUserEmail(authentication);
        Map<String, Object> details = Map.of(
            "action", "SAVE_OR_UPDATE_NO_REPLY_MAILBOX",
            "requestedEmail", dto.getEmailAddress()
        );

        try {
            NoReplyMailboxDto saved = mailboxService.saveOrUpdateMailbox(dto, actorEmail);

            emitAuditEvent(
                "NO_REPLY_MAILBOX_SAVE",
                String.valueOf(saved.getId()),
                saved.getEmailAddress(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                details,
                "SUCCESS"
            );

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            emitAuditEvent(
                "NO_REPLY_MAILBOX_SAVE",
                dto.getId() != null ? String.valueOf(dto.getId()) : "UNKNOWN",
                dto.getEmailAddress(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("error", e.getMessage()),
                "FAILURE"
            );
            throw e;
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMailbox(Authentication authentication) {
        String actorEmail = resolveUserEmail(authentication);
        Map<String, Object> details = Map.of("action", "DELETE_NO_REPLY_MAILBOX");

        try {
            mailboxService.deleteMailbox(actorEmail);

            emitAuditEvent(
                "NO_REPLY_MAILBOX_DELETE",
                "1",
                "No-Reply Mailbox Configuration",
                ActorType.USER,
                actorEmail,
                actorEmail,
                details,
                "SUCCESS"
            );

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            emitAuditEvent(
                "NO_REPLY_MAILBOX_DELETE",
                "1",
                "No-Reply Mailbox Configuration",
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
                    .module("EMAIL_MANAGER")
                    .targetEntityType("NoReplyMailbox")
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