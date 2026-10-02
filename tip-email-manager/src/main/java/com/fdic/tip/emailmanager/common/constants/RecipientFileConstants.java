package com.fdic.tip.emailmanager.common.constants;

/** Limits and messages for the FILE_UPLOAD recipient mode (uploaded recipient spreadsheet). */
public final class RecipientFileConstants {

    private RecipientFileConstants() {
    }

    public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10MB, same ceiling as email attachments
    public static final String[] ALLOWED_EXTENSIONS = {"xlsx", "csv"};

    public static final String MSG_FILE_TOO_LARGE = "Recipient file exceeds the maximum allowed size of 10MB.";
    public static final String MSG_FILE_TYPE_NOT_ALLOWED = "Recipient file must be .xlsx or .csv.";
    public static final String MSG_NO_SHEETS_FOUND = "No worksheets found in the uploaded recipient file.";
    public static final String MSG_SHEET_NOT_FOUND = "Selected sheet was not found in the uploaded recipient file.";
    public static final String MSG_NO_FILE_UPLOADED = "Upload a recipient file before selecting a sheet.";
    public static final String MSG_NO_SHEET_SELECTED = "Select a worksheet before mapping recipient columns.";
    public static final String MSG_EMPTY_SHEET = "The selected sheet has no header row.";
}
