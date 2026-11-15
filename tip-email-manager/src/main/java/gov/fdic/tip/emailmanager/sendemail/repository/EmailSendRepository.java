package gov.fdic.tip.emailmanager.sendemail.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.EmailSendStatus;
import gov.fdic.tip.emailmanager.sendemail.entity.EmailSend;

public interface EmailSendRepository extends JpaRepository<EmailSend, Long> {

    // User's own sends in reverse chronological order
    Page<EmailSend> findBySubmittedByOrderBySubmittedAtDesc(String submittedBy, Pageable pageable);

    // Reviewer queue for External Recipient Sends
    Page<EmailSend> findByStatusAndExternalRecipientsGreaterThanOrderBySubmittedAtDesc(
            EmailSendStatus status, int minExternalRecipients, Pageable pageable);
}