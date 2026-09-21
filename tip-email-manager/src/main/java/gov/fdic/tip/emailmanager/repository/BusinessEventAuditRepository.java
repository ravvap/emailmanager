package gov.fdic.tip.emailmanager.repository;

import gov.fdic.tip.emailmanager.entity.BusinessEventAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for business event audit records.
 * Provides custom query methods for target entity label matching.
 */

@Repository
public interface BusinessEventAuditRepository extends JpaRepository<BusinessEventAudit, UUID>, JpaSpecificationExecutor<BusinessEventAudit> {
	/**
     * Finds audit records where target_entity_label contains the specified text (case-insensitive).
     *
     * @param targetEntityLabel search string (e.g., "Email Manager")
     * @param pageable pagination and sorting parameters
     * @return page of matching audit entities
     */
    Page<BusinessEventAudit> findByTargetEntityLabelContainingIgnoreCase(String targetEntityLabel, Pageable pageable);
    
}