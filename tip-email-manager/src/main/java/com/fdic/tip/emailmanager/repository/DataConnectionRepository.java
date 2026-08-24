package com.fdic.tip.emailmanager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fdic.tip.emailmanager.entity.DataConnection;

@Repository
public interface DataConnectionRepository extends JpaRepository<DataConnection, Long> {
	// Standard Spring Data JPA method
    boolean existsByName(String name);

    // Soft-delete aware method (checks only active, non-deleted records)
    boolean existsByNameAndDeletedAtIsNull(String name);

    // Soft-delete aware method for updates (excludes current ID)
    boolean existsByNameAndIdNotAndDeletedAtIsNull(String name, Long id);

    Optional<DataConnection> findByIdAndDeletedAtIsNull(Long id);

    List<DataConnection> findByDeletedAtIsNull();

    List<DataConnection> findByStatusAndAuthorUserIdsContainingAndDeletedAtIsNull(String status, Long userId);
    
    
}