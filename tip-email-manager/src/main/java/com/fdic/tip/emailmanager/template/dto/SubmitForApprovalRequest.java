package com.fdic.tip.emailmanager.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

/**
 * Step 4 — Preview and Submit. Approval Comments are required here —
 * this is the submitter's note to the reviewer for a NEW_TEMPLATE/EDIT
 * change request, distinct from EM-10's Retire/Reactivate reason, which
 * stays optional per that AC.
 */
@Value
@Builder
public class SubmitForApprovalRequest {

    @NotBlank
    @Size(max = 500)
    String comments;
}
