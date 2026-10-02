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

    // Only meaningful (and validated) for DATA_SOURCE_QUERY mode, where
    // these map directly to the pinned query's columns. FILE_UPLOAD will
    // eventually map these to the uploaded file's header row instead —
    // not yet implemented; ignored for CONTACT_DISTRIBUTION_LIST (recipient
    // name comes from the contact record) and DEFINE_AT_SEND (no mapping yet).
    String recipientEmailColumn;
    String recipientNameColumn;

    List<Long> distributionListIds;
    List<Long> contactIds;
}
