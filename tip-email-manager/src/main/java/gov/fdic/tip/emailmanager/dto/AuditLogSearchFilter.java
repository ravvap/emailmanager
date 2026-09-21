package gov.fdic.tip.emailmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Filter criteria DTO for searching business audit logs.
 * Maps user UI inputs to backend query parameters.
 *
 * @author FDIC Email Manager Platform
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogSearchFilter {

    /** Start timestamp bound for Date/Time filtering */
    private OffsetDateTime startDateTime;

    /** End timestamp bound for Date/Time filtering */
    private OffsetDateTime endDateTime;

    /** Filters by User (matches actorId or actorLabel) */
    private String user;

    /** Filters by Functionality (maps to module)[cite: 44] */
    private String functionality;

    /** Filters by Activity (matches eventType or targetEntityLabel)[cite: 44] */
    private String activity;

    /** Generic free-text search across details, user, functionality, and activity */
    private String searchKeyword;
}