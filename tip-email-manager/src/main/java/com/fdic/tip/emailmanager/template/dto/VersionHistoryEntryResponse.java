package com.fdic.tip.emailmanager.template.dto;

import com.fdic.tip.emailmanager.template.enums.VersionStatus;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

/** One row of EM-11's version history list. */
@Value
@Builder
public class VersionHistoryEntryResponse {

    Long templateVersionId;
    Integer versionNumber;
    VersionStatus status;
    String subject;
    String submittedBy;
    OffsetDateTime submittedAt;
    String decidedBy;
    OffsetDateTime decidedAt;
    Long restoredFromVersionNumber;
}
