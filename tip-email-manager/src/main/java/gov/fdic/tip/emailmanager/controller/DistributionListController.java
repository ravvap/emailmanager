package gov.fdic.tip.emailmanager.controller;

import gov.fdic.tip.emailmanager.constant.AppConstants;
import gov.fdic.tip.emailmanager.constant.ActorType;
import gov.fdic.tip.emailmanager.dto.*;
import gov.fdic.tip.emailmanager.service.AuditLogService;
import gov.fdic.tip.emailmanager.service.DistributionListService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/distribution-lists")
public class DistributionListController {

    private final DistributionListService distributionListService;
    private final AuditLogService auditLogService;

    public DistributionListController(DistributionListService distributionListService, AuditLogService auditLogService) {
        this.distributionListService = distributionListService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize(AppConstants.PERM_DISTRIBUTION_LIST_VIEW)
    public ResponseEntity<List<DistributionListSummaryDto>> getAllDistributionLists(Authentication authentication) {
        String actorEmail = authentication.getName();
        log.info("Fetching distribution list summaries by user: {}", actorEmail);

        List<DistributionListSummaryDto> list = distributionListService.getAllDistributionListsSummary();

        auditLogService.emitAuditEvent(
                "DISTRIBUTION_LIST_READ_ALL",
                "ALL",
                "Distribution Lists Summary List",
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("count", list.size()),
                "SUCCESS"
        );

        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_DISTRIBUTION_LIST_VIEW)
    public ResponseEntity<DistributionListViewDto> getDistributionListDetails(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        String actorEmail = authentication.getName();
        log.info("Viewing distribution list details ID: {} by user: {}", id, actorEmail);

        Pageable pageable = PageRequest.of(page, size);
        DistributionListViewDto viewDto = distributionListService.getDistributionListDetails(id, filter, pageable);

        auditLogService.emitAuditEvent(
                "DISTRIBUTION_LIST_READ_DETAILS",
                String.valueOf(id),
                viewDto.getName(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("listId", id, "filter", filter, "page", page, "size", size),
                "SUCCESS"
        );

        return ResponseEntity.ok(viewDto);
    }

    @PostMapping
    @PreAuthorize(AppConstants.PERM_DISTRIBUTION_LIST_ADD)
    public ResponseEntity<DistributionListSummaryDto> createDistributionList(
            @Valid @RequestBody CreateDistributionListRequest request,
            Authentication authentication) {
        String actorEmail = authentication.getName();
        log.info("Creating distribution list: {} by user: {}", request.getName(), actorEmail);

        DistributionListSummaryDto created = distributionListService.createDistributionList(request, actorEmail);

        auditLogService.emitAuditEvent(
                "DISTRIBUTION_LIST_CREATE",
                String.valueOf(created.getId()),
                created.getName(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("listName", created.getName(), "memberCount", created.getMemberCount()),
                "SUCCESS"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_DISTRIBUTION_LIST_EDIT)
    public ResponseEntity<DistributionListSummaryDto> updateDistributionList(
            @PathVariable Long id,
            @Valid @RequestBody CreateDistributionListRequest request,
            Authentication authentication) {
        String actorEmail = authentication.getName();
        log.info("Updating distribution list ID: {} by user: {}", id, actorEmail);

        DistributionListSummaryDto updated = distributionListService.updateDistributionList(id, request, actorEmail);

        auditLogService.emitAuditEvent(
                "DISTRIBUTION_LIST_UPDATE",
                String.valueOf(id),
                updated.getName(),
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("listId", id, "memberCount", updated.getMemberCount()),
                "SUCCESS"
        );

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_DISTRIBUTION_LIST_DELETE)
    public ResponseEntity<Void> deleteDistributionList(
            @PathVariable Long id,
            Authentication authentication) {
        String actorEmail = authentication.getName();
        log.info("Deleting distribution list ID: {} by user: {}", id, actorEmail);

        distributionListService.deleteDistributionList(id, actorEmail);

        auditLogService.emitAuditEvent(
                "DISTRIBUTION_LIST_DELETE",
                String.valueOf(id),
                "Distribution List " + id,
                ActorType.USER,
                actorEmail,
                actorEmail,
                Map.of("listId", id),
                "SUCCESS"
        );

        return ResponseEntity.noContent().build();
    }
}