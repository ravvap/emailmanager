package gov.fdic.tip.emailmanager.sendemail.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.BounceCategory;
import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.RecipientDeliveryStatus;
import gov.fdic.tip.emailmanager.sendemail.entity.EmailSendRecipient;

public interface EmailSendRecipientRepository extends JpaRepository<EmailSendRecipient, Long> {

    List<EmailSendRecipient> findByEmailSend_SendIdOrderBySendRecipientIdAsc(Long sendId);

    List<EmailSendRecipient> findByEmailSend_SendIdAndDeliveryStatus(Long sendId, RecipientDeliveryStatus status);

    long countByRecipientEmailIgnoreCaseAndBounceCategory(String recipientEmail, BounceCategory bounceCategory);
}