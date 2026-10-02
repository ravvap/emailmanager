package com.fdic.tip.emailmanager.template.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RecipientSheetSelectionRequest {

    @NotBlank
    String sheetName;
}
