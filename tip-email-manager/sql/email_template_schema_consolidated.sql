-- =====================================================================
-- TIP Email Manager — Email Template module (EM-8 through EM-13)
-- CONSOLIDATED schema — current end-state only, V1 through V6 merged.
-- Schema: txn
--
-- This file is a reference snapshot, not a migration — it is not meant
-- to be run against a database that already has V1-V6 applied (every
-- CREATE/ALTER in those files already produced this exact state). Keep
-- using the V1__...sql through V6__...sql files for actual Flyway
-- deployment; regenerate this file (or a V7+ consolidated one) next
-- time enough changes pile up that reading six migrations in sequence
-- stops being the fastest way to see the current shape.
--
-- Table-by-table origin, for traceability back to the migration that
-- introduced or changed it:
--   email_template               V1 (base), V6 (soft delete, partial unique index)
--   email_template_version       V1 (base), V2 (lifecycle statuses, approval fields moved out,
--                                 recipient-mapping conflict flag, restore provenance),
--                                 V3 (FILE_UPLOAD file/sheet columns, storage-path approach),
--                                 V4 (switched FILE_UPLOAD storage from blob path to in-row BYTEA)
--   email_template_attachment    V1 — unchanged; still blob storage + virus scan (email
--                                 attachments are served back to recipients, unlike the
--                                 recipient file, which never leaves the database)
--   email_template_merge_field   V1
--   email_template_recipient_selection  V1
--   email_template_file_recipient       V5 (new) — parsed recipient rows
--   email_template_change_request       V2 (new)
--   email_template_audit_log     V1
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS txn;

-- =====================================================================
-- email_template : logical, named template (owner, current pointer)
-- =====================================================================
CREATE TABLE txn.email_template (
    template_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_name           VARCHAR(150)      NOT NULL,
    status                  VARCHAR(20)       NOT NULL DEFAULT 'DRAFT',
    owner_user_id            VARCHAR(100)     NOT NULL,
    current_version_id      BIGINT            NULL,
    active_version_id       BIGINT            NULL,
    created_by              VARCHAR(100)      NOT NULL,
    created_at               TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_by               VARCHAR(100)      NOT NULL,
    updated_at               TIMESTAMPTZ       NOT NULL DEFAULT now(),
    deleted_by               VARCHAR(100),
    deleted_at               TIMESTAMPTZ,

    -- DRAFT: never had an approved version. ACTIVE: has a serving
    -- version. RETIRED: taken out of active use (EM-10, reversible via
    -- Reactivate). A pending change or a rejected draft lives on the
    -- version/change-request rows, not here. Deleted is deleted_at IS
    -- NOT NULL, not a status value — only permitted from DRAFT/RETIRED.
    CONSTRAINT ck_email_template_status
        CHECK (status IN ('DRAFT','ACTIVE','RETIRED'))
);

-- Deleted templates' names are free to reuse.
CREATE UNIQUE INDEX uq_email_template_name_active
    ON txn.email_template (LOWER(template_name))
    WHERE deleted_at IS NULL;

CREATE INDEX idx_email_template_deleted_at
    ON txn.email_template (deleted_at)
    WHERE deleted_at IS NOT NULL;

COMMENT ON TABLE txn.email_template IS 'Logical template header; owner + pointer to current/active version. Name unique among non-deleted templates.';

-- =====================================================================
-- email_template_version : one content snapshot per version
-- =====================================================================
CREATE TABLE txn.email_template_version (
    template_version_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_id              BIGINT            NOT NULL REFERENCES txn.email_template(template_id),
    version_number           INT               NOT NULL,

    -- Template Details step
    from_identity_id         BIGINT            NOT NULL,

    -- References data_source_query.asset_id (EM-1/EM-19 schema), not any
    -- single version row's own id. No DB-level FK: asset_id isn't unique
    -- on its own in data_source_query (each version is its own row
    -- sharing asset_id) — enforced in the service layer instead. Together
    -- with data_source_query_version, this pins the exact (asset_id,
    -- version) row this template was approved against.
    data_source_query_id      UUID              NOT NULL,
    data_source_query_version INT               NOT NULL,

    -- Email Content step
    subject                  VARCHAR(255)      NOT NULL,
    body_html                TEXT              NOT NULL,
    body_plain_text          TEXT              NOT NULL,
    body_size_bytes          INT               NOT NULL,

    -- Recipient mapping (one of four modes; see RecipientMode)
    recipient_mode           VARCHAR(30)       NOT NULL,
    recipient_email_column   VARCHAR(100),
    recipient_name_column    VARCHAR(100),

    -- FILE_UPLOAD mode only — stored directly in this row (BYTEA), not
    -- in blob storage. NOT virus-scanned: parsed straight into
    -- email_template_file_recipient and never served back to a user,
    -- unlike email_template_attachment.
    recipient_file_name      VARCHAR(255),
    recipient_file_content   BYTEA,
    recipient_sheet_name     VARCHAR(255),

    -- Lifecycle (submission/approval bookkeeping lives on
    -- email_template_change_request, not here)
    status                   VARCHAR(20)       NOT NULL DEFAULT 'DRAFT',

    -- Query-version drift flags (cannot submit until resolved)
    has_merge_field_conflict          BOOLEAN  NOT NULL DEFAULT FALSE,
    has_recipient_mapping_conflict    BOOLEAN  NOT NULL DEFAULT FALSE,
    newer_query_version_available     INT,

    -- EM-12: when this version came from a restore, the version it was copied from
    restored_from_version_id BIGINT REFERENCES txn.email_template_version(template_version_id),

    created_by                VARCHAR(100)      NOT NULL,
    created_at                TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_by                VARCHAR(100)      NOT NULL,
    updated_at                TIMESTAMPTZ       NOT NULL DEFAULT now(),

    CONSTRAINT uq_template_version UNIQUE (template_id, version_number),
    CONSTRAINT ck_template_version_status
        CHECK (status IN ('DRAFT','PENDING_APPROVAL','ACTIVE','SUPERSEDED','REJECTED','WITHDRAWN')),
    CONSTRAINT ck_recipient_mode
        CHECK (recipient_mode IN ('CONTACT_DISTRIBUTION_LIST','DATA_SOURCE_QUERY','FILE_UPLOAD','DEFINE_AT_SEND')),
    CONSTRAINT ck_subject_length CHECK (char_length(subject) <= 255)
);

ALTER TABLE txn.email_template
    ADD CONSTRAINT fk_email_template_current_version
        FOREIGN KEY (current_version_id) REFERENCES txn.email_template_version(template_version_id),
    ADD CONSTRAINT fk_email_template_active_version
        FOREIGN KEY (active_version_id) REFERENCES txn.email_template_version(template_version_id);

CREATE INDEX ix_email_template_version_template_id ON txn.email_template_version(template_id);
CREATE INDEX ix_email_template_version_status ON txn.email_template_version(status);

-- =====================================================================
-- email_template_attachment : stored with the version (cannot change
-- on an already-approved version — a new version is created instead).
-- Unlike the recipient file above, this IS virus-scanned and stored in
-- blob storage, because it is served back to recipients as an email
-- attachment.
-- =====================================================================
CREATE TABLE txn.email_template_attachment (
    attachment_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_version_id      BIGINT            NOT NULL REFERENCES txn.email_template_version(template_version_id),
    file_name                VARCHAR(255)      NOT NULL,
    file_extension            VARCHAR(10)       NOT NULL,
    file_size_bytes           BIGINT            NOT NULL,
    storage_path              VARCHAR(500)      NOT NULL,
    virus_scan_status         VARCHAR(20)       NOT NULL DEFAULT 'PENDING',
    uploaded_by                VARCHAR(100)      NOT NULL,
    uploaded_at                TIMESTAMPTZ       NOT NULL DEFAULT now(),

    CONSTRAINT ck_attachment_scan_status
        CHECK (virus_scan_status IN ('PENDING','CLEAN','INFECTED','FAILED')),
    CONSTRAINT ck_attachment_size CHECK (file_size_bytes <= 10485760)
);

CREATE INDEX ix_attachment_version_id ON txn.email_template_attachment(template_version_id);

-- =====================================================================
-- email_template_merge_field : placeholders inserted into subject/body,
-- tied to a real query column at insert time; flagged if that column
-- later disappears
-- =====================================================================
CREATE TABLE txn.email_template_merge_field (
    merge_field_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_version_id       BIGINT            NOT NULL REFERENCES txn.email_template_version(template_version_id),
    placeholder_name          VARCHAR(100)      NOT NULL,
    source_column             VARCHAR(100)      NOT NULL,
    field_location             VARCHAR(10)       NOT NULL,
    is_broken                  BOOLEAN           NOT NULL DEFAULT FALSE,

    CONSTRAINT ck_merge_field_location CHECK (field_location IN ('SUBJECT','BODY'))
);

CREATE INDEX ix_merge_field_version_id ON txn.email_template_merge_field(template_version_id);

-- =====================================================================
-- email_template_recipient_selection : chosen distribution lists /
-- contacts for CONTACT_DISTRIBUTION_LIST mode. Validated against the
-- distribution-list/contacts directory endpoints at save time, not
-- enforced here as a DB FK (those tables are managed by EM-1 proper).
-- =====================================================================
CREATE TABLE txn.email_template_recipient_selection (
    selection_id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_version_id        BIGINT            NOT NULL REFERENCES txn.email_template_version(template_version_id),
    distribution_list_id       BIGINT,
    contact_id                  BIGINT,
    added_at                    TIMESTAMPTZ       NOT NULL DEFAULT now(),

    CONSTRAINT ck_recipient_selection_one_of
        CHECK (
            (distribution_list_id IS NOT NULL AND contact_id IS NULL) OR
            (distribution_list_id IS NULL AND contact_id IS NOT NULL)
        )
);

CREATE INDEX ix_recipient_selection_version_id ON txn.email_template_recipient_selection(template_version_id);

-- =====================================================================
-- email_template_file_recipient : parsed recipient rows (name + email)
-- out of a FILE_UPLOAD-mode file's mapped sheet (sourced from
-- email_template_version.recipient_file_content). Populated once a
-- valid column mapping is chosen; replaced wholesale on any re-upload,
-- re-sheet, or re-mapping.
-- =====================================================================
CREATE TABLE txn.email_template_file_recipient (
    file_recipient_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_version_id      BIGINT            NOT NULL REFERENCES txn.email_template_version(template_version_id),
    row_number                INT               NOT NULL,
    recipient_name            VARCHAR(255),
    recipient_email           VARCHAR(255),
    is_valid                  BOOLEAN           NOT NULL DEFAULT TRUE,
    validation_error          VARCHAR(255),

    CONSTRAINT uq_file_recipient_row UNIQUE (template_version_id, row_number)
);

CREATE INDEX idx_file_recipient_version_id ON txn.email_template_file_recipient (template_version_id);
CREATE INDEX idx_file_recipient_invalid
    ON txn.email_template_file_recipient (template_version_id)
    WHERE is_valid = FALSE;

-- =====================================================================
-- email_template_change_request : the single maker-checker gate over
-- every template change type (EM-13): NEW_TEMPLATE, EDIT, RETIRE,
-- REACTIVATE, RESTORE. Approver must differ from submitter — enforced
-- both here (DB CHECK) and in the service layer.
-- =====================================================================
CREATE TABLE txn.email_template_change_request (
    change_request_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_id              BIGINT            NOT NULL REFERENCES txn.email_template(template_id),
    change_type              VARCHAR(20)       NOT NULL,
    template_version_id      BIGINT            REFERENCES txn.email_template_version(template_version_id),
    restore_from_version_id  BIGINT            REFERENCES txn.email_template_version(template_version_id),

    status                    VARCHAR(20)       NOT NULL DEFAULT 'PENDING',
    reason                    VARCHAR(500),
    rejection_reason          VARCHAR(500),

    submitted_by               VARCHAR(100)      NOT NULL,
    submitted_at               TIMESTAMPTZ       NOT NULL DEFAULT now(),
    decided_by                 VARCHAR(100),
    decided_at                 TIMESTAMPTZ,
    decision_comments          VARCHAR(500),

    CONSTRAINT ck_change_request_type
        CHECK (change_type IN ('NEW_TEMPLATE','EDIT','RETIRE','REACTIVATE','RESTORE')),
    CONSTRAINT ck_change_request_status
        CHECK (status IN ('PENDING','APPROVED','REJECTED','WITHDRAWN')),
    CONSTRAINT ck_change_request_version_required
        CHECK (
            (change_type IN ('NEW_TEMPLATE','EDIT','RESTORE') AND template_version_id IS NOT NULL)
            OR (change_type IN ('RETIRE','REACTIVATE') AND template_version_id IS NULL)
        ),
    CONSTRAINT ck_change_request_no_self_approval
        CHECK (decided_by IS NULL OR decided_by <> submitted_by)
);

CREATE INDEX idx_change_request_pending_queue
    ON txn.email_template_change_request (status, change_type, submitted_at);
CREATE INDEX idx_change_request_template_id
    ON txn.email_template_change_request (template_id);

-- Only one pending content change (new/edit/restore) per template at a time.
CREATE UNIQUE INDEX uq_one_pending_content_change_per_template
    ON txn.email_template_change_request (template_id)
    WHERE status = 'PENDING' AND change_type IN ('NEW_TEMPLATE','EDIT','RESTORE');

-- Only one pending lifecycle change (retire/reactivate) per template at a time.
CREATE UNIQUE INDEX uq_one_pending_lifecycle_change_per_template
    ON txn.email_template_change_request (template_id)
    WHERE status = 'PENDING' AND change_type IN ('RETIRE','REACTIVATE');

-- =====================================================================
-- email_template_audit_log : full audit trail
-- =====================================================================
CREATE TABLE txn.email_template_audit_log (
    audit_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_id         BIGINT            NOT NULL REFERENCES txn.email_template(template_id),
    template_version_id BIGINT            REFERENCES txn.email_template_version(template_version_id),
    action              VARCHAR(50)       NOT NULL,
    performed_by         VARCHAR(100)      NOT NULL,
    performed_at         TIMESTAMPTZ       NOT NULL DEFAULT now(),
    detail               VARCHAR(1000)
);

CREATE INDEX ix_audit_log_template_id ON txn.email_template_audit_log(template_id);
