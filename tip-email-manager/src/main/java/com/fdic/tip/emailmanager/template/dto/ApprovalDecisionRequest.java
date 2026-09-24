package com.fdic.tip.emailmanager.template.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

/** EM-13 approval/rejection decision by the checker. */
@Value
@Builder
public class ApprovalDecisionRequest {

    @Size(max = 500)
    String comments;

    @Size(max = 500)
    String rejectionReason; // required only when rejecting
}
