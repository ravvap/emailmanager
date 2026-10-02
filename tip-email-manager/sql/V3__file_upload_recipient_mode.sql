-- =====================================================================
-- TIP Email Manager — Email Template module, V3
-- FILE_UPLOAD recipient mode (Preview-and-Submit screen's Recipients
-- section: File / Sheet / Recipient Name / Recipient Email, all mapped
-- off the uploaded file's own columns — previously had no columns to
-- persist into at all).
--
-- recipient_email_column / recipient_name_column already exist and are
-- reused as-is here: for FILE_UPLOAD they hold the mapped file column
-- name instead of a query column name (same two columns, different
-- source of truth depending on recipient_mode).
-- =====================================================================

ALTER TABLE txn.email_template_version
    ADD COLUMN recipient_file_name          VARCHAR(255),
    ADD COLUMN recipient_file_storage_path  VARCHAR(500),
    ADD COLUMN recipient_sheet_name         VARCHAR(255);

COMMENT ON COLUMN txn.email_template_version.recipient_file_name IS
    'Original uploaded file name for FILE_UPLOAD recipient mode (Preview screen "File:").';
COMMENT ON COLUMN txn.email_template_version.recipient_file_storage_path IS
    'Blob storage path of the uploaded recipient file — same storage/virus-scan gate as email_template_attachment, kept separate since this is recipient data, not an email attachment.';
COMMENT ON COLUMN txn.email_template_version.recipient_sheet_name IS
    'Selected worksheet within the uploaded file (Preview screen "Sheet:"); the sheet whose header row recipient_email_column / recipient_name_column are validated against.';
