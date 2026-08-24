package com.fdic.tip.emailmanager.controller;

import com.fdic.tip.emailmanager.dto.DomainAllowlistDto;
import com.fdic.tip.emailmanager.service.InternalDomainAllowlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal-domains")
@RequiredArgsConstructor
@Tag(name = "Internal Domain Allowlist Controller", description = "Endpoints for managing allowlisted internal email domains")
public class InternalDomainAllowlistController {

    private final InternalDomainAllowlistService service;

    @PostMapping
    @Operation(summary = "Create a new allowlisted domain")
    public ResponseEntity<DomainAllowlistDto> createDomain(
            @Valid @RequestBody DomainAllowlistDto dto,
            @RequestHeader(value = "X-User-Name", defaultValue = "SYSTEM") String username) {
        DomainAllowlistDto created = service.createDomain(dto, username);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing allowlisted domain")
    public ResponseEntity<DomainAllowlistDto> updateDomain(
            @PathVariable Long id,
            @Valid @RequestBody DomainAllowlistDto dto,
            @RequestHeader(value = "X-User-Name", defaultValue = "SYSTEM") String username) {
        DomainAllowlistDto updated = service.updateDomain(id, dto, username);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete an allowlisted domain")
    public ResponseEntity<Void> deleteDomain(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Name", defaultValue = "SYSTEM") String username) {
        service.deleteDomain(id, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get all active allowlisted domains")
    public ResponseEntity<List<DomainAllowlistDto>> getAllDomains() {
        return ResponseEntity.ok(service.getAllDomains());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an allowlisted domain by ID")
    public ResponseEntity<DomainAllowlistDto> getDomainById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getDomainById(id));
    }
}