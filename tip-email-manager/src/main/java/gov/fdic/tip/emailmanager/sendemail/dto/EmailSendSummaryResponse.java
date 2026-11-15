package gov.fdic.tip.emailmanager.sendemail.dto;

import java.time.OffsetDateTime;

import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.EmailSendStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailSendSummaryResponse {
    private Long sendId;
    private Long templateId;
    private String templateName;
    private int totalRecipients;
    private int externalRecipients;
    private EmailSendStatus status;
    private String submittedBy;
    private OffsetDateTime submittedAt;
    private OffsetDateTime completedAt;
}