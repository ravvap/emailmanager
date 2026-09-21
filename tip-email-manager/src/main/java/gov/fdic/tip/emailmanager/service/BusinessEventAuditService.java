package gov.fdic.tip.emailmanager.service;

import gov.fdic.tip.emailmanager.dto.AuditLogResponseDto;
import gov.fdic.tip.emailmanager.dto.AuditLogSearchFilter;
import gov.fdic.tip.emailmanager.entity.BusinessEventAudit;
import gov.fdic.tip.emailmanager.repository.BusinessEventAuditRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service Implementation for Business Event Audit Log Querying and Persistence.
 *
 * <p>Provides multi-field filtering capabilities across Date/Time, User, Functionality,
 * Activity, and JSON Details parameters (EM-18)[cite: 44, 45].
 *
 * @author FDIC Email Manager Platform
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class BusinessEventAuditService {

    private final BusinessEventAuditRepository auditRepository;

    /**
     * Persists an append-only audit event to the business_events_audit table[cite: 44, 45].
     *
     * @param auditEntry entity containing event metadata[cite: 44]
     * @return persisted entity record[cite: 44]
     */
    @Transactional
    public BusinessEventAudit logEvent(BusinessEventAudit auditEntry) {
        auditEntry.setTimestamp(OffsetDateTime.now());
        return auditRepository.save(auditEntry);
    }

    /**
     * Queries and filters audit logs based on user search criteria[cite: 45].
     *
     * @param filter DTO containing dateTime, user, functionality, activity, and search criteria
     * @param pageable pagination details
     * @return page of formatted AuditLogResponseDto items
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> searchAuditLogs(AuditLogSearchFilter filter, Pageable pageable) {
        Specification<BusinessEventAudit> spec = createSearchSpecification(filter);
        return auditRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    /**
     * Constructs dynamic JPA Predicates mapping UI search fields to DB schema columns[cite: 44].
     *
     * Field Mapping Summary:
     * - Date/Time     -> timestamp[cite: 44]
     * - User          -> actorLabel / actorId[cite: 44]
     * - Functionality -> module[cite: 44]
     * - Activity      -> eventType / targetEntityLabel[cite: 44]
     * - Keyword       -> details (JSONB string representation)[cite: 44]
     */
    private Specification<BusinessEventAudit> createSearchSpecification(AuditLogSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Date/Time Range Filter (timestamp)[cite: 44]
            if (filter.getStartDateTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), filter.getStartDateTime()));
            }
            if (filter.getEndDateTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), filter.getEndDateTime()));
            }

            // 2. User Filter (actorLabel or actorId)[cite: 44]
            if (filter.getUser() != null && !filter.getUser().isBlank()) {
                String userPattern = "%" + filter.getUser().toLowerCase() + "%";
                Predicate matchActorLabel = cb.like(cb.lower(root.get("actorLabel")), userPattern);
                Predicate matchActorId = cb.like(cb.lower(root.get("actorId")), userPattern);
                predicates.add(cb.or(matchActorLabel, matchActorId));
            }

            // 3. Functionality Filter (module)[cite: 44]
            if (filter.getFunctionality() != null && !filter.getFunctionality().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("module")), filter.getFunctionality().toLowerCase()));
            }

            // 4. Activity Filter (eventType or targetEntityLabel)[cite: 44]
            if (filter.getActivity() != null && !filter.getActivity().isBlank()) {
                String activityPattern = "%" + filter.getActivity().toLowerCase() + "%";
                Predicate matchEventType = cb.like(cb.lower(root.get("eventType")), activityPattern);
                Predicate matchTargetLabel = cb.like(cb.lower(root.get("targetEntityLabel")), activityPattern);
                predicates.add(cb.or(matchEventType, matchTargetLabel));
            }

            // 5. Keyword Search across User, Functionality, Activity, and JSON Details[cite: 44]
            if (filter.getSearchKeyword() != null && !filter.getSearchKeyword().isBlank()) {
                String pattern = "%" + filter.getSearchKeyword().toLowerCase() + "%";

                Predicate searchActorLabel = cb.like(cb.lower(root.get("actorLabel")), pattern);
                Predicate searchActorId = cb.like(cb.lower(root.get("actorId")), pattern);
                Predicate searchModule = cb.like(cb.lower(root.get("module")), pattern);
                Predicate searchEventType = cb.like(cb.lower(root.get("eventType")), pattern);
                Predicate searchTargetLabel = cb.like(cb.lower(root.get("targetEntityLabel")), pattern);
                
                // Cast PostgreSQL jsonb column 'details' to string for pattern searching[cite: 44]
                Predicate searchDetailsJson = cb.like(
                        cb.lower(root.get("details").as(String.class)), pattern
                );

                predicates.add(cb.or(
                        searchActorLabel, 
                        searchActorId, 
                        searchModule, 
                        searchEventType, 
                        searchTargetLabel, 
                        searchDetailsJson
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Maps database entity fields to UI Response DTO presentation format[cite: 44].
     */
    private AuditLogResponseDto mapToDto(BusinessEventAudit audit) {
        String userDisplay = audit.getActorLabel() != null && !audit.getActorLabel().isBlank() 
                ? audit.getActorLabel() 
                : audit.getActorId();

        String activityDisplay = audit.getEventType() 
                + (audit.getTargetEntityLabel() != null ? " - " + audit.getTargetEntityLabel() : "");

        return AuditLogResponseDto.builder()
                .id(audit.getId())
                .dateTime(audit.getTimestamp())
                .user(userDisplay)
                .functionality(audit.getModule())
                .activity(activityDisplay)
                .events(audit.getDetails())
                .build();
    }
}