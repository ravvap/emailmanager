package com.fdic.tip.emailmanager.common.constants;

/** Config keys, path conventions, and messages for the attachment storage/virus-scan adapter. */
public final class AttachmentStorageConstants {

    private AttachmentStorageConstants() {
    }

    // application.yml keys (see AttachmentStoragePortImpl @Value bindings)
    public static final String PROP_CONTAINER_NAME = "tip.email-manager.attachments.container-name";
    public static final String PROP_VIRUS_SCAN_URL = "tip.email-manager.attachments.virus-scan-url";
    public static final String PROP_VIRUS_SCAN_TIMEOUT_MS = "tip.email-manager.attachments.virus-scan-timeout-ms";

    public static final String DEFAULT_CONTAINER_NAME = "email-template-attachments";
    public static final String BLOB_PATH_PREFIX = "templates";
    public static final int DEFAULT_VIRUS_SCAN_TIMEOUT_MS = 15_000;

    // Messages
    public static final String MSG_SCAN_INFECTED = "Attachment failed the virus scan and was not stored.";
    public static final String MSG_SCAN_FAILED = "Attachment virus scan could not be completed. Please try again.";
    public static final String MSG_UPLOAD_FAILED = "Attachment could not be stored. Please try again.";
    public static final String MSG_EMPTY_FILE = "Attachment file is empty.";

    public static final String VIRUS_SCAN_VERDICT_HEADER = "X-Scan-Verdict";
}
