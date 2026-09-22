package gov.fdic.tip.emailmanager.controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fdic.tip.emailmanager.constant.AppConstants;

import gov.fdic.tip.emailmanager.dto.CreateQueryRequest;
import gov.fdic.tip.emailmanager.dto.DataSourceQueryResponse;
import gov.fdic.tip.emailmanager.service.DataSourceQueryService;
import jakarta.validation.Valid;

/**
 * REST Endpoints for Data Source Query governance actions.
 *
 * @author prasad ravva
 */
@RestController
@RequestMapping("/api/v1/data-source-queries")
public class DataSourceQueryController {

    private final DataSourceQueryService service;

    public DataSourceQueryController(DataSourceQueryService service) {
        this.service = service;
    }

    /**
     * Fetches all queries across versions for the query dashboard.
     */
    @GetMapping
    @PreAuthorize(AppConstants.PERM_VIEW_QUERIES)
    public ResponseEntity<List<DataSourceQueryResponse>> listAllQueries() {
        return ResponseEntity.ok(service.listAllQueries());
    }

    /**
     * Fetches complete version history for a specific query asset.
     */
    @GetMapping("/history/{assetId}")
    @PreAuthorize(AppConstants.PERM_VIEW_QUERIES)
    public ResponseEntity<List<DataSourceQueryResponse>> getAssetHistory(@PathVariable UUID assetId) {
        return ResponseEntity.ok(service.getAssetVersionHistory(assetId));
    }

    /**
     * Submits a new Data Source Query (v1).
     */
    @PostMapping
    @PreAuthorize(AppConstants.PERM_MANAGE_QUERIES)
    public ResponseEntity<DataSourceQueryResponse> addQuery(
            @Valid @RequestBody CreateQueryRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.addQuery(request, principal.getName()));
    }

    /**
     * Creates a new version (vN+1) by editing an active query.
     */
    @PostMapping("/{versionId}/edit")
    @PreAuthorize(AppConstants.PERM_MANAGE_QUERIES)
    public ResponseEntity<DataSourceQueryResponse> editQuery(
            @PathVariable UUID versionId,
            @Valid @RequestBody CreateQueryRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.editQuery(versionId, request, principal.getName()));
    }

    /**
     * Approves a pending query version.
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize(AppConstants.PERM_MANAGE_QUERIES)
    public ResponseEntity<DataSourceQueryResponse> approveQuery(
            @PathVariable UUID id,
            Principal principal) {
        return ResponseEntity.ok(service.approveQuery(id, principal.getName()));
    }

    /**
     * Rejects a pending query version.
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize(AppConstants.PERM_MANAGE_QUERIES)
    public ResponseEntity<DataSourceQueryResponse> rejectQuery(
            @PathVariable UUID id,
            Principal principal) {
        return ResponseEntity.ok(service.rejectQuery(id, principal.getName()));
    }

    /**
     * Retires an active query version (Soft Delete).
     */
    @PutMapping("/{id}/retire")
    @PreAuthorize(AppConstants.PERM_MANAGE_QUERIES)
    public ResponseEntity<DataSourceQueryResponse> retireQuery(@PathVariable UUID id) {
        return ResponseEntity.ok(service.retireQuery(id));
    }

    /**
     * Deletes an unreferenced query version permanently (Hard Delete).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_MANAGE_QUERIES)
    public ResponseEntity<Void> deleteQuery(@PathVariable UUID id) {
        service.deleteQuery(id);
        return ResponseEntity.noContent().build();
    }
}