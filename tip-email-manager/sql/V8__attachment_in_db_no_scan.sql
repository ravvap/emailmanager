-- =====================================================================
-- TIP Email Manager — Email Template module, V8
-- Removes remote blob storage and virus scanning entirely. Attachments
-- are now stored in-row (BYTEA), same as the FILE_UPLOAD recipient file
-- (V4). storage_path and virus_scan_status (+ its CHECK) are dropped.
--
-- NOTE: existing rows have no bytes to carry over (their content lived
-- in blob storage). On a database that already holds attachments, back-
-- fill file_content from blob storage BEFORE running this; on a fresh
-- environment, nothing to migrate.
-- =====================================================================
ALTER TABLE txn.email_template_attachment
    DROP CONSTRAINT ck_attachment_scan_status,
    DROP COLUMN storage_path,
    DROP COLUMN virus_scan_status,
    ADD COLUMN file_content BYTEA NOT NULL;
