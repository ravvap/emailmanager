package gov.fdic.tip.emailmanager.repository;

import gov.fdic.tip.emailmanager.entity.BusinessEventAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BusinessEventAuditRepository extends JpaRepository<BusinessEventAudit, UUID>, JpaSpecificationExecutor<BusinessEventAudit> {
    // Append-only repository; modification methods (save/delete) should be strictly controlled by audit logic
}