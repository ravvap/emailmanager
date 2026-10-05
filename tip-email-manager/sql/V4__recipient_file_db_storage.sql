-- =====================================================================
-- TIP Email Manager — Email Template module, V4
-- Switches the FILE_UPLOAD recipient file from external blob storage
-- (V3's recipient_file_storage_path, pointing at Azure Blob Storage) to
-- being stored directly in this table as a BYTEA column. No virus scan
-- on this file: it is parsed into email_template_file_recipient rows
-- (see V5) and never executed, downloaded, or served back as-is, unlike
-- email_template_attachment, which is still scanned and stored in blob
-- storage.
-- =====================================================================

ALTER TABLE txn.email_template_version
    DROP COLUMN recipient_file_storage_path,
    ADD COLUMN recipient_file_content BYTEA;

COMMENT ON COLUMN txn.email_template_version.recipient_file_content IS
    'Raw bytes of the uploaded FILE_UPLOAD recipient file, stored in-row rather than in blob storage. Not virus-scanned — parsed directly into email_template_file_recipient, never served back to a user.';
