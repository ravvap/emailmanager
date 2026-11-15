package gov.fdic.tip.emailmanager.sendemail.constants;

public final class AppConstants {

    private AppConstants() {}

    // ===================================================================
    // Operational Thresholds & Limits
    // ===================================================================
    public static final int PERMANENT_BOUNCE_THRESHOLD = 3;
    public static final int APPROVAL_COMMENTS_MAX_LENGTH = 500;
    public static final String DEFAULT_EXCEL_SHEET_NAME = "Sheet1";
    public static final String DEFAULT_EMAIL_COLUMN = "email";
    public static final String DEFAULT_NAME_COLUMN = "name";
    public static final String FAILED_DELIVERIES_CSV_HEADER = "Recipient Name,Recipient Email,Failure Reason\n";

    // ===================================================================
    // Validation & Error Messages
    // ===================================================================
    public static final String MSG_SEND_NOT_FOUND = "Email send record not found.";
    public static final String MSG_SEND_RECIPIENT_NOT_FOUND = "Send recipient record not found.";
    public static final String MSG_TEMPLATE_NOT_ACTIVE = "Only approved, active template versions can be sent.";
    public static final String MSG_RECIPIENT_CONFLICT = "Conflicting merge values detected for one or more recipient email addresses. Resolve conflicts before submitting.";
    public static final String MSG_NO_VALID_RECIPIENTS = "At least one valid recipient is required to submit a send.";
    public static final String MSG_SELF_APPROVAL_BLOCKED = "Submitters cannot approve or reject their own email send.";
    public static final String MSG_NOT_PENDING_REVIEW = "Email send is not in Pending Review status.";
    public static final String MSG_REJECTION_REASON_REQUIRED = "Rejection reason is required (max 500 characters).";
    public static final String MSG_CANCEL_NOT_ALLOWED = "Send cannot be canceled once delivery has started or after a reviewer decision.";
    public static final String MSG_CANCEL_UNAUTHORIZED = "Only the original submitter can cancel this email send.";
    public static final String MSG_CSV_EXPORT_ONLY_COMPLETED = "Failed deliveries CSV export is only available for Completed sends.";
    public static final String MSG_FILE_UPLOAD_NOT_ALLOWED = "This template is not configured for Excel recipient upload.";
    public static final String MSG_VIRUS_SCAN_FAILED = "Uploaded recipient file failed virus scan.";

    // ===================================================================
    // Delivery Outcome Display Messages
    // ===================================================================
    public static final String OUTCOME_NOT_DISPATCHED = "These emails have not been dispatched.";
    public static final String OUTCOME_IN_PROGRESS = "Email dispatch is currently in progress.";
    public static final String OUTCOME_COMPLETED_FORMAT = "Completed: %d delivered, %d failed.";

    // ===================================================================
    // Audit Actions
    // ===================================================================
    public static final String AUDIT_SUBMITTED = "SUBMITTED";
    public static final String AUDIT_CANCELED = "CANCELED";
    public static final String AUDIT_APPROVED = "APPROVED";
    public static final String AUDIT_REJECTED = "REJECTED";
    public static final String AUDIT_RE_APPROVAL_REQUIRED = "RE_APPROVAL_REQUIRED";

    // ===================================================================
    // Consolidated Enums
    // ===================================================================
    public enum EmailSendStatus {
        PENDING_REVIEW,
        REJECTED_BY_REVIEWER,
        CANCELED,
        QUEUED,
        IN_PROGRESS,
        COMPLETED
    }

    public enum RecipientClassification {
        INTERNAL,
        EXTERNAL
    }

    public enum RecipientDeliveryStatus {
        PENDING,
        SENT,
        DELIVERED,
        RETRYING,
        FAILED_PERMANENT,
        SKIPPED_INVALID
    }

    public enum BounceCategory {
        NONE,
        TEMPORARY,
        PERMANENT
    }
}