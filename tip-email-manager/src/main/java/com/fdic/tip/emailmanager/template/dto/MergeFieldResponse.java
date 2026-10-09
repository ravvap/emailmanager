package com.fdic.tip.emailmanager.template.dto;

import com.fdic.tip.emailmanager.template.enums.MergeFieldLocation;
import lombok.Builder;
import lombok.Value;

/** A merge field on a version, with whether its source column is still returned by the pinned query version. */
@Value
@Builder
public class MergeFieldResponse {

    String placeholderName;
    String sourceColumn;
    MergeFieldLocation fieldLocation;
    boolean broken;
}
