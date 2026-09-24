package com.fdic.tip.emailmanager.template.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

/** Step 4 — Preview and Submit. */
@Value
@Builder
public class SubmitForApprovalRequest {

    @Size(max = 500)
    String comments;
}
