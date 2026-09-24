package com.fdic.tip.emailmanager.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

/** Step 1 of the Author Template wizard — Template Details. */
@Value
@Builder
public class TemplateDetailsRequest {

    @NotBlank
    @Size(max = 150)
    String templateName;

    @NotNull
    Long fromIdentityId;

    @NotNull
    Long dataSourceQueryId;
}
