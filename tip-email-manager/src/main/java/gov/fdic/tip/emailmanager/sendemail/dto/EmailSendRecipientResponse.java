package gov.fdic.tip.emailmanager.sendemail.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.*;

@Data
@Builder
public class EmailSendRecipientResponse {
    private Long sendRecipientId;
    private String recipientName;
    private String recipientEmail;
    private RecipientClassification classification;
    private RecipientDeliveryStatus deliveryStatus;
    private BounceCategory bounceCategory;
    private String failureReason;
    private boolean flaggedForContactReview;
    private OffsetDateTime deliveredAt;
}