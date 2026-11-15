package gov.fdic.tip.emailmanager.sendemail.dto;

import com.fdic.tip.emailmanager.template.enums.RecipientMode;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SendPreviewResponse {
    private Long templateId;
    private Long templateVersionId;
    private String templateName;
    private Long fromIdentityId;
    private RecipientMode recipientMode;

    private String subject;
    // Rendered with 1st row of sample data from query/file/list
    private String renderedSampleBodyHtml;
    private List<String> attachmentFileNames;

    private int totalRecipients;
    private int internalRecipients;
    private int externalRecipients;
    private boolean requiresExternalApproval;

    private boolean hasAddressConflict;
    private List<String> conflictingAddresses;

    private List<EmailSendRecipientResponse> recipients;
    private List<String> skippedInvalidRows;
}