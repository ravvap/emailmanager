package gov.fdic.tip.emailmanager.sendemail.dto;

import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.EmailSendStatus;
 import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSendDetailResponse {
    private Long sendId;
    private Long templateId;
    private Long templateVersionId;
    private String templateName;
    private RecipientMode recipientMode;

    private EmailSendStatus status;
    private String deliveryOutcomeMessage;
    private boolean canCancel;
    private boolean canExportFailedCsv;

    private int totalRecipients;
    private int externalRecipients;
    private int deliveredCount;
    private int failedCount;

    private String subject;
    private String renderedSampleBodyHtml;
    private List<String> attachmentFileNames;

    private String submittedBy;
    private OffsetDateTime submittedAt;
    private String reviewedBy;
    private OffsetDateTime reviewedAt;
    private String reviewComments;
    private OffsetDateTime canceledAt;
    private OffsetDateTime completedAt;

    private List<EmailSendRecipientResponse> recipients;
}