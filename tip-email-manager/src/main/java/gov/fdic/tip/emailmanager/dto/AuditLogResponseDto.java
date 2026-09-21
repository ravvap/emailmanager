package gov.fdic.tip.emailmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for Audit Log UI Grid presentation (EM-18)[cite: 45].
 *
 * @author FDIC Email Manager Platform
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDto {

    /** Unique audit event identifier[cite: 44] */
    private UUID id;

    /** Date and time when the event occurred (mapped from 'timestamp')[cite: 44] */
    private OffsetDateTime dateTime;

    /** User who initiated the action (mapped from 'actorLabel' / 'actorId')[cite: 44] */
    private String user;

    /** Application functionality/module affected (mapped from 'module')[cite: 44] */
    private String functionality;

    /** Activity action and target entity (mapped from 'eventType' + 'targetEntityLabel')[cite: 44] */
    private String activity;

    /** Contextual event details (mapped from jsonb 'details')[cite: 44] */
    private Map<String, Object> events;
}