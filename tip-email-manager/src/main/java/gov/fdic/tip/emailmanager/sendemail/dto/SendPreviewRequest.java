package gov.fdic.tip.emailmanager.sendemail.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SendPreviewRequest {
    @NotNull
    private Long templateId;

    // Used when RecipientMode == DATA_SOURCE_QUERY
    private Map<String, Object> queryParameters;

    // Used when RecipientMode == FILE_UPLOAD
    private String uploadSessionToken;

    // Used when RecipientMode == DEFINE_AT_SEND
    private List<AdHocRecipientRequest> adHocRecipients;
}