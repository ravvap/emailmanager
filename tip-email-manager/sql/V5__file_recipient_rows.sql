-- =====================================================================
-- TIP Email Manager — Email Template module, V5
-- Parses and persists the actual recipient rows (name + email) out of
-- the FILE_UPLOAD recipient file's mapped sheet, rather than
-- re-parsing recipient_file_content at send time. Populated once the
-- author picks a valid column mapping in updateRecipients(); replaced
-- wholesale on any re-upload, re-sheet, or re-mapping.
-- =====================================================================

CREATE TABLE txn.email_template_file_recipient (
    file_recipient_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_version_id      BIGINT            NOT NULL REFERENCES txn.email_template_version(template_version_id),
    row_number                INT               NOT NULL,  -- 1-based position within the sheet, excluding the header row
    recipient_name            VARCHAR(255),
    recipient_email           VARCHAR(255),
    is_valid                  BOOLEAN           NOT NULL DEFAULT TRUE,
    validation_error          VARCHAR(255),      -- e.g. "missing email", "malformed email address"

    CONSTRAINT uq_file_recipient_row UNIQUE (template_version_id, row_number)
);

CREATE INDEX idx_file_recipient_version_id
    ON txn.email_template_file_recipient (template_version_id);

CREATE INDEX idx_file_recipient_invalid
    ON txn.email_template_file_recipient (template_version_id)
    WHERE is_valid = FALSE;

COMMENT ON TABLE txn.email_template_file_recipient IS
    'Parsed recipient rows (name, email) for FILE_UPLOAD recipient mode — one row per non-header row in the mapped sheet, replaced wholesale on re-upload/re-map.';
