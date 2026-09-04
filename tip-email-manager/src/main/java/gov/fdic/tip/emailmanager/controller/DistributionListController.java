package gov.fdic.tip.emailmanager.controller;

import gov.fdic.tip.emailmanager.constant.AppConstants;
import gov.fdic.tip.emailmanager.dto.DistributionListDto;
import gov.fdic.tip.emailmanager.dto.DistributionListMemberDto;
import gov.fdic.tip.emailmanager.service.DistributionListService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/distribution-lists")
public class DistributionListController {

    private final DistributionListService distributionListService;

    public DistributionListController(DistributionListService distributionListService) {
        this.distributionListService = distributionListService;
    }

 // GET endpoint for the "View Distribution List" modal
    @GetMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_DISTRIBUTION_LIST_VIEW)
    public ResponseEntity<DistributionListViewDto> getDistributionListDetails(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        DistributionListViewDto viewDto = distributionListService.getDistributionListDetails(id, filter, pageable);
        
        return ResponseEntity.ok(viewDto);
    }
    
    // GET all distribution lists
    @GetMapping
    @PreAuthorize(AppConstants.PERM_DISTRIBUTION_LIST_VIEW)
    public ResponseEntity<List<DistributionListDto>> getAllDistributionLists() {
        return ResponseEntity.ok(distributionListService.getAllDistributionLists());
    }

    // GET distribution list details by ID
    @GetMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_DISTRIBUTION_LIST_VIEW)
    public ResponseEntity<DistributionListDto> getDistributionListById(@PathVariable Long id) {
        return ResponseEntity.ok(distributionListService.getDistributionListById(id));
    }

    // GET filter distribution list members by Name/Email query with pagination
    @GetMapping("/{id}/members")
    @PreAuthorize(AppConstants.PERM_DISTRIBUTION_LIST_VIEW)
    public ResponseEntity<Page<DistributionListMemberDto>> filterMembers(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(distributionListService.filterMembers(id, query, PageRequest.of(page, size)));
    }

    // POST create distribution list
    @PostMapping
    @PreAuthorize(AppConstants.PERM_DISTRIBUTION_LIST_ADD)
    public ResponseEntity<DistributionListDto> createDistributionList(
            @Valid @RequestBody DistributionListDto dto,
            Authentication authentication) {
        DistributionListDto created = distributionListService.createDistributionList(dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT update distribution list
    @PutMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_DISTRIBUTION_LIST_EDIT)
    public ResponseEntity<DistributionListDto> updateDistributionList(
            @PathVariable Long id,
            @Valid @RequestBody DistributionListDto dto,
            Authentication authentication) {
        DistributionListDto updated = distributionListService.updateDistributionList(id, dto, authentication.getName());
        return ResponseEntity.ok(updated);
    }

    // DELETE distribution list (hard delete if unused, soft delete if referenced in send history)
    @DeleteMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_DISTRIBUTION_LIST_DELETE)
    public ResponseEntity<Void> deleteDistributionList(
            @PathVariable Long id,
            Authentication authentication) {
        distributionListService.deleteDistributionList(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}