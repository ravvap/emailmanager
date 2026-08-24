package com.fdic.tip.emailmanager.controller;

import java.security.Principal;
import java.util.List;

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
import com.fdic.tip.emailmanager.dto.ConnectionTestResultDto;
import com.fdic.tip.emailmanager.dto.DataConnectionDto;
import com.fdic.tip.emailmanager.service.DataConnectionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller providing REST API endpoints for Data Connection operations.
 */
@RestController
@RequestMapping("/api/v1/data-connections")
@RequiredArgsConstructor
@Tag(name = "Data Connections", description = "APIs for managing database data connections")
public class DataConnectionController {

    private final DataConnectionService service;

    /**
     * Retrieves all active, non-deleted Data Connections.
     *
     * @return List of Data Connections
     */
    @Operation(summary = "Get all data connections", description = "Retrieves all active data connections that have not been soft deleted.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data connections retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    @GetMapping
    @PreAuthorize(AppConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<List<DataConnectionDto>> getAllConnections() {
        return ResponseEntity.ok(service.getAllConnections());
    }

    /**
     * Creates a new Data Connection entity.
     *
     * @param dto Data payload
     * @param principal Principal user context
     * @return Created DataConnectionDto
     */
    @Operation(summary = "Create data connection", description = "Creates a new data connection with assigned authors.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Data connection created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate name"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    @PostMapping
    @PreAuthorize(AppConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<DataConnectionDto> createConnection(@Valid @RequestBody DataConnectionDto dto, Principal principal) {
        return new ResponseEntity<>(service.createConnection(dto, principal.getName()), HttpStatus.CREATED);
    }

    /**
     * Updates an existing Data Connection entity.
     *
     * @param id Connection ID
     * @param dto Updated payload
     * @param principal Principal user context
     * @return Updated DataConnectionDto
     */
    @Operation(summary = "Update data connection", description = "Updates details and configuration of an existing data connection.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data connection updated successfully"),
        @ApiResponse(responseCode = "404", description = "Connection record not found"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PutMapping("/{id}")
    @PreAuthorize(AppConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<DataConnectionDto> updateConnection(@PathVariable Long id, @Valid @RequestBody DataConnectionDto dto, Principal principal) {
        return ResponseEntity.ok(service.updateConnection(id, dto, principal.getName()));
    }

    /**
     * Soft-deletes a Data Connection entity.
     *
     * @param id Connection ID to soft-delete
     * @param principal Principal user context
     * @return Confirmation message
     */
    @Operation(summary = "Soft delete data connection", description = "Performs a soft delete on a data connection if it is INACTIVE and not used in a template.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data connection soft deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot delete active connection or connection linked to a template"),
        @ApiResponse(responseCode = "404", description = "Connection record not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize(AppConstants.HAS_ROLE_ADMIN)
    public ResponseEntity<String> deleteConnection(@PathVariable Long id, Principal principal) {
        service.deleteConnection(id, principal.getName());
        return ResponseEntity.ok(AppConstants.MSG_CONNECTION_DELETED);
    }
    
    @PostMapping("/test")
    @Operation(summary = "Test database connection details before saving")
    public ResponseEntity<ConnectionTestResultDto> testConnection(
            @Valid @RequestBody DataConnectionDto dto) {
        ConnectionTestResultDto result = service.testConnection(dto);
        return ResponseEntity.ok(result);
    }
}