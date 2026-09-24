package com.fdic.tip.emailmanager.template.dto;

import com.fdic.tip.emailmanager.template.enums.ChangeRequestStatus;
import com.fdic.tip.emailmanager.template.enums.ChangeType;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

/** A row in the approval queue (EM-13). */
@Value
@Builder
public class ChangeRequestResponse {

    Long changeRequestId;
    Long templateId;
    String templateName;
    ChangeType changeType;
    ChangeRequestStatus status;
    Long templateVersionId;
    Integer versionNumber;
    String reason;
    String rejectionReason;
    String submittedBy;
    OffsetDateTime submittedAt;
    String decidedBy;
    OffsetDateTime decidedAt;
    String decisionComments;
}
