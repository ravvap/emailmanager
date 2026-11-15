package gov.fdic.tip.emailmanager.sendemail.repository;

import gov.fdic.tip.emailmanager.sendemail.entity.EmailSendAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailSendAuditLogRepository extends JpaRepository<EmailSendAuditLog, Long> {
}