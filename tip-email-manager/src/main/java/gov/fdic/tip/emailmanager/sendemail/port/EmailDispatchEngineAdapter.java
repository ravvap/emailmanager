package gov.fdic.tip.emailmanager.sendemail.port;

import gov.fdic.tip.emailmanager.sendemail.client.NotificationClient;
import gov.fdic.tip.emailmanager.sendemail.dto.NotificationSendRequest;
import gov.fdic.tip.emailmanager.sendemail.entity.EmailSend;
import gov.fdic.tip.emailmanager.sendemail.repository.EmailSendRepository;
import gov.fdic.tip.emailmanager.sendemail.repository.EmailSendRecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailDispatchEngineAdapter implements EmailDispatchEnginePort {

    private final EmailSendRepository sendRepository;
    private final EmailSendRecipientRepository sendRecipientRepository;
    private final NotificationClient notificationClient; // Feign Client injected here

    @Override
    @Async
    public void enqueueSend(Long sendId) {
        log.info("Starting dispatch engine execution for sendId: {}", sendId);

        EmailSend send = sendRepository.findById(sendId)
                .orElseThrow(() -> new IllegalStateException("EmailSend not found for dispatch: " + sendId));

        send.setStatus(gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.EmailSendStatus.IN_PROGRESS);
        sendRepository.save(send);

        var recipients = sendRecipientRepository.findByEmailSend_SendIdOrderBySendRecipientIdAsc(sendId);

        int delivered = 0;
        int failed = 0;

        for (var recipient : recipients) {
            try {
                NotificationSendRequest payload = NotificationSendRequest.builder()
                        .emailAddress(recipient.getRecipientEmail())
                        .subject(send.getFrozenSubject())
                        .body(send.getFrozenBodyHtml())
                        .build();

                // Call notification service via Feign client
                notificationClient.sendEmail(payload);

                recipient.setDeliveryStatus(gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.RecipientDeliveryStatus.DELIVERED);
                recipient.setDeliveredAt(OffsetDateTime.now());
                delivered++;
            } catch (Exception e) {
                log.error("Failed to send email to {}: {}", recipient.getRecipientEmail(), e.getMessage());
                recipient.setDeliveryStatus(gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.RecipientDeliveryStatus.FAILED);
                recipient.setFailureReason(e.getMessage());
                failed++;
            }
            sendRecipientRepository.save(recipient);
        }

        send.setDeliveredCount(delivered);
        send.setFailedCount(failed);
        send.setStatus(gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.EmailSendStatus.COMPLETED);
        send.setCompletedAt(OffsetDateTime.now());
        sendRepository.save(send);

        log.info("Completed dispatch execution for sendId: {}. Delivered: {}, Failed: {}", sendId, delivered, failed);
    }
}