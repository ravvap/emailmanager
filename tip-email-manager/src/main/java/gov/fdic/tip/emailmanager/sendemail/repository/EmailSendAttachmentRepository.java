package gov.fdic.tip.emailmanager.sendemail.repository;

import gov.fdic.tip.emailmanager.sendemail.entity.EmailSendAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailSendAttachmentRepository extends JpaRepository<EmailSendAttachment, Long> {
}