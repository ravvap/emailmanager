package com.fdic.tip.emailmanager.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/** Step 2 of the Author Template wizard — Email Content. */
@Value
@Builder
public class EmailContentRequest {

    @NotBlank
    @Size(max = 255)
    String subject;

    @NotBlank
    String bodyHtml;

    List<MergeFieldRequest> mergeFields;
}
