package gov.fdic.tip.emailmanager.sendemail.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExternalApprovalDecisionRequest {
    @NotNull
    private Boolean approved;

    @Size(max = 500)
    private String comments;
}