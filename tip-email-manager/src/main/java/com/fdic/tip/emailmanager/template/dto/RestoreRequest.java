package com.fdic.tip.emailmanager.template.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

/** EM-12: restore a prior approved/superseded version. */
@Value
@Builder
public class RestoreRequest {

    @NotNull
    Long sourceVersionId;
}
