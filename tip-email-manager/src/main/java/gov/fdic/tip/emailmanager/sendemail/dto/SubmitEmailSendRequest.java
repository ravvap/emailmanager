package gov.fdic.tip.emailmanager.sendemail.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SubmitEmailSendRequest {
    @NotNull
    private Long templateId;

    private Map<String, Object> queryParameters;
    private String uploadSessionToken;
    private List<AdHocRecipientRequest> adHocRecipients;
}