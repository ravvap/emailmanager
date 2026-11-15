package gov.fdic.tip.emailmanager.sendemail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdHocRecipientRequest {
    private String recipientName;

    @NotBlank
    @Email
    private String recipientEmail;

    private Map<String, String> mergeValues;
}