package gov.fdic.tip.emailmanager.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.fdic.tip.emailmanager.dto.AuditLogResponseDto;
import gov.fdic.tip.emailmanager.dto.AuditLogSearchFilter;
import gov.fdic.tip.emailmanager.service.BusinessEventAuditService;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller for Audit Log Operations (EM-18)[cite: 45].
 *
 * <p>Provides secure, read-only search and querying capabilities over the system business
 * audit trail[cite: 45, 47]. Access is strictly restricted to users holding Internal Auditor
 * or System Administrator roles[cite: 45].
 *
 * @author FDIC Email Manager Platform
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('INTERNAL_AUDITOR', 'SYS_ADMIN')") // Role-based access enforcement[cite: 45]
public class BusinessEventAuditController {

    private final BusinessEventAuditService auditService;

    /**
     * Searches and filters audit log entries for grid display[cite: 45, 47].
     *
     * @param filter request body containing date range, actor, module, or keyword filters[cite: 47]
     * @param pageable pagination and sorting details (defaults to descending by timestamp)[cite: 47]
     * @return paginated list of mapped audit log records[cite: 47]
     */
    @PostMapping("/search")
    public ResponseEntity<Page<AuditLogResponseDto>> searchAuditLogs(
            @RequestBody(required = false) AuditLogSearchFilter filter,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        
        if (filter == null) {
            filter = new AuditLogSearchFilter();
        }
        Page<AuditLogResponseDto> auditLogs = auditService.searchAuditLogs(filter, pageable);
        return ResponseEntity.ok(auditLogs);
    }
}