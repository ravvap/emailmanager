package com.fdic.tip.emailmanager.template.dto;

import com.fdic.tip.emailmanager.template.enums.RecipientMode;
import com.fdic.tip.emailmanager.template.enums.TemplateStatus;
import com.fdic.tip.emailmanager.template.enums.VersionStatus;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Full Preview-and-Submit shape. */
@Value
@Builder
public class EmailTemplateDetailResponse {

    Long templateId;
    String templateName;
    TemplateStatus templateStatus;

    Long templateVersionId;
    Integer versionNumber;
    VersionStatus versionStatus;

    String fromDisplayName;
    UUID dataSourceQueryId;
    Integer dataSourceQueryVersion;
    Boolean hasMergeFieldConflict;
    Boolean hasRecipientMappingConflict;
    List<MergeFieldResponse> mergeFields;
    Integer newerQueryVersionAvailable;

    String subject;
    String bodyHtml;

    RecipientMode recipientMode;
    String recipientEmailColumn;
    String recipientNameColumn;

    // FILE_UPLOAD mode only — Preview screen's "File:" / "Sheet:" fields.
    // recipientEmailColumn/recipientNameColumn above double as the
    // "mapped file column" fields for this mode.
    String recipientFileName;
    String recipientSheetName;
    Integer recipientFileRowCount;
    Integer recipientFileInvalidRowCount;

    List<String> attachmentFileNames;
    List<String> selectedRecipientNames;

    // The current/most-recent change request for this version, if one
    // exists — carries the Preview-and-Submit screen's "Approval Comments"
    // field (the submitter's note, saved as of submission) and, once a
    // decision has been made, the reviewer's side of it. All null for a
    // version still in DRAFT that's never been submitted.
    String approvalComments;
    String decisionComments;
    String rejectionReason;
    String decidedBy;
    OffsetDateTime decidedAt;
}
