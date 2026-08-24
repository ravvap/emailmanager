package com.fdic.tip.emailmanager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fdic.tip.emailmanager.entity.DatabaseLocation;

/**
 * Repository interface for DatabaseLocation entity operations.
 */
@Repository
public interface DatabaseLocationRepository extends JpaRepository<DatabaseLocation, Long> {

    /**
     * Finds an active DatabaseLocation by ID excluding soft-deleted records.
     *
     * @param id The primary key ID.
     * @return Optional containing the found DatabaseLocation or empty if soft-deleted/not found.
     */
    Optional<DatabaseLocation> findByIdAndDeletedAtIsNull(Long id);

    /**
     * Retrieves all active DatabaseLocation records excluding soft-deleted ones.
     *
     * @return List of non-deleted DatabaseLocation entities.
     */
    List<DatabaseLocation> findByDeletedAtIsNull();

    /**
     * Checks if a record exists with the specified hostname, port, and service name, excluding soft-deleted items.
     *
     * @param hostname The database host.
     * @param port The database port.
     * @param serviceName The database service name.
     * @return True if a matching active record exists, false otherwise.
     */
    boolean existsByHostnameAndPortAndServiceNameAndDeletedAtIsNull(String hostname, Integer port, String serviceName);
}