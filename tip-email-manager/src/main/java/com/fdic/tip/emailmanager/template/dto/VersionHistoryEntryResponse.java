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

    // Same fields EmailTemplateDetailResponse carries for the current
    // version — surfaced per history row too, since EM-11 is explicitly
    // "who submitted and approved each, and when" and a reviewer's
    // rejection reason is part of that record.
    String approvalComments;
    String decisionComments;
    String rejectionReason;
}
