package com.fdic.tip.emailmanager.common.constants;

/**
 * Centralized constants for the Email Template module — field limits and
 * user-facing messages kept out of business logic per team convention.
 */
public final class EmailTemplateConstants {

    private EmailTemplateConstants() {
    }

    // ---- Field limits ----
    public static final int TEMPLATE_NAME_MAX_LENGTH = 150;
    public static final int SUBJECT_MAX_LENGTH = 255;
    public static final int APPROVAL_COMMENTS_MAX_LENGTH = 500;
    public static final long ATTACHMENT_MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10MB
    public static final long INLINE_IMAGE_MAX_SIZE_BYTES = 2L * 1024 * 1024; // 2MB, target — confirm with team
    public static final int BODY_MAX_SIZE_BYTES = 512 * 1024; // ceiling to confirm with team

    // ---- Messages ----
    public static final String MSG_TEMPLATE_NAME_DUPLICATE = "Template name must be unique.";
    public static final String MSG_TEMPLATE_NOT_FOUND = "Email template not found.";
    public static final String MSG_VERSION_NOT_FOUND = "Email template version not found.";
    public static final String MSG_NOT_EDITABLE = "This template version is not editable in its current status.";
    public static final String MSG_MERGE_FIELD_CONFLICT =
            "One or more merge fields reference a column no longer returned by the pinned query version. Resolve before submitting.";
    public static final String MSG_UNAUTHORIZED_QUERY =
            "You are not authorized to reference this data source query.";
    public static final String MSG_ATTACHMENT_TOO_LARGE = "Attachment exceeds the maximum allowed size of 10MB.";
    public static final String MSG_ATTACHMENT_TYPE_NOT_ALLOWED = "Attachment file type is not permitted.";
    public static final String MSG_ALREADY_PENDING_APPROVAL = "This template version is already pending approval.";
    public static final String MSG_SUBMIT_SUCCESS = "Template submitted for approval.";
    public static final String MSG_APPROVE_SUCCESS = "Template version approved and is now active.";
    public static final String MSG_REJECT_SUCCESS = "Template version rejected.";

    // ---- V2: EM-9 / EM-10 / EM-11 / EM-12 / EM-13 ----
    public static final String MSG_TEMPLATE_NOT_ACTIVE_FOR_EDIT = "Only an active template can be edited.";
    public static final String MSG_TEMPLATE_NOT_ACTIVE_FOR_RETIRE = "Only an active template can be retired.";
    public static final String MSG_TEMPLATE_NOT_RETIRED_FOR_REACTIVATE = "Only a retired template can be reactivated.";
    public static final String MSG_PENDING_CHANGE_EXISTS = "This template already has a pending change awaiting approval.";
    public static final String MSG_RESTORE_SOURCE_INVALID = "The selected version is not eligible to be restored from.";
    public static final String MSG_SELF_APPROVAL_BLOCKED = "You cannot approve or reject your own submission.";
    public static final String MSG_CHANGE_REQUEST_NOT_FOUND = "Change request not found.";
    public static final String MSG_CHANGE_REQUEST_NOT_PENDING = "This change request is no longer pending.";
    public static final String MSG_WITHDRAW_NOT_ALLOWED = "Only the submitter, a Manager, or a TIP Administrator may withdraw this request.";
    public static final String MSG_ANALYST_HISTORY_RESTRICTED = "Analysts may only view the active version's history.";
    public static final String MSG_COMPARE_REQUIRES_HISTORY_ACCESS = "You do not have access to compare template versions.";

    // ---- Allowed attachment extensions ----
    public static final String[] ALLOWED_ATTACHMENT_EXTENSIONS = {"docx", "xlsx"};
}
