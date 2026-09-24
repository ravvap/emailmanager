package com.fdic.tip.emailmanager.template.dto;

import com.fdic.tip.emailmanager.template.enums.MergeFieldLocation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MergeFieldRequest {

    @NotBlank
    String placeholderName;

    @NotBlank
    String sourceColumn;

    @NotNull
    MergeFieldLocation fieldLocation;
}
