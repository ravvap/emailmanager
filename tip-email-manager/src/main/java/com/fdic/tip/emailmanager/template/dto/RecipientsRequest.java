package com.fdic.tip.emailmanager.template.dto;

import com.fdic.tip.emailmanager.template.enums.RecipientMode;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/** Step 3 of the Author Template wizard — Recipients Mode. */
@Value
@Builder
public class RecipientsRequest {

    @NotNull
    RecipientMode recipientMode;

    String recipientEmailColumn;   // required for DATA_SOURCE_QUERY / FILE_UPLOAD modes
    String recipientNameColumn;    // required only for query-driven personalized greeting

    List<Long> distributionListIds;
    List<Long> contactIds;
}
