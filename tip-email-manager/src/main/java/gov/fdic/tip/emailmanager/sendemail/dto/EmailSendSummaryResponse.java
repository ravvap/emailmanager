package gov.fdic.tip.emailmanager.sendemail.dto;

import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.EmailSendStatus;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
public class EmailSendSummaryResponse {
    private Long sendId;
    private Long templateId;
    private String templateName;
    private Integer totalRecipients;
    private Integer deliveredCount;
    private Integer failedCount;
    private EmailSendStatus status;
    private String submittedBy;
    private OffsetDateTime submittedAt;
}