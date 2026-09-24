package com.fdic.tip.emailmanager.template.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

/** EM-10: retire or reactivate request — reason is optional per AC. */
@Value
@Builder
public class LifecycleRequest {

    @Size(max = 500)
    String reason;
}
