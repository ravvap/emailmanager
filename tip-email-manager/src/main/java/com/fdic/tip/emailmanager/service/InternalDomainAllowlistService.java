package com.fdic.tip.emailmanager.service;

import java.util.List;

import com.fdic.tip.emailmanager.dto.DomainAllowlistDto;

/**
 * Service interface for managing Internal Domain Allowlist entries.
 */
public interface InternalDomainAllowlistService {

    /**
     * Creates a new domain allowlist entry.
     *
     * @param dto      The domain allowlist details.
     * @param username The user performing the creation.
     * @return The created DomainAllowlistDto.
     */
    DomainAllowlistDto createDomain(DomainAllowlistDto dto, String username);

    /**
     * Updates an existing domain allowlist entry.
     *
     * @param id       The ID of the domain entry to update.
     * @param dto      The updated domain details.
     * @param username The user performing the update.
     * @return The updated DomainAllowlistDto.
     */
    DomainAllowlistDto updateDomain(Long id, DomainAllowlistDto dto, String username);

    /**
     * Soft deletes an allowlisted domain entry.
     *
     * @param id       The ID of the domain entry to delete.
     * @param username The user performing the deletion.
     */
    void deleteDomain(Long id, String username);

    /**
     * Retrieves all active domain allowlist entries excluding soft-deleted ones.
     *
     * @return List of DomainAllowlistDto objects.
     */
    List<DomainAllowlistDto> getAllDomains();

    /**
     * Retrieves a single allowlisted domain entry by ID.
     *
     * @param id The primary key ID.
     * @return The found DomainAllowlistDto.
     */
    DomainAllowlistDto getDomainById(Long id);
}