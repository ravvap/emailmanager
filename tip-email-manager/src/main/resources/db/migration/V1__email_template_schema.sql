-- =====================================================================
-- TIP Email Manager — Email Template module (EM-8 / EM-13)
-- Schema: txn
-- Mirrors conventions used in EM-1 (Data Connections): audit columns,
-- soft status gating (Active/Inactive), FK to sender identities and
-- data source queries owned by that module.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS txn;

-- ---------------------------------------------------------------------
-- Lookup / enum-backed check constraints (kept as VARCHAR + CHECK to
-- match existing EM-1 convention rather than native postgres ENUM, so
-- new values don't require a type migration).
-- ---------------------------------------------------------------------

-- txn.sender_identity and txn.data_source_query / txn.data_source_query_version
-- are assumed to already exist from EM-1 / EM-19. Referenced by FK below;
-- if their actual names differ, adjust the FK targets accordingly.

-- =====================================================================
-- email_template : the logical, named template (owner, current pointer)
-- =====================================================================
CREATE TABLE txn.email_template (
    template_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_name           VARCHAR(150)      NOT NULL,
    status                  VARCHAR(20)       NOT NULL DEFAULT 'DRAFT',
    owner_user_id            VARCHAR(100)     NOT NULL,
    current_version_id      BIGINT            NULL,      -- FK added after email_template_version exists
    active_version_id       BIGINT            NULL,      -- last APPROVED & currently sendable version
    created_by              VARCHAR(100)      NOT NULL,
    created_at               TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_by               VARCHAR(100)      NOT NULL,
    updated_at               TIMESTAMPTZ       NOT NULL DEFAULT now(),

    CONSTRAINT uq_email_template_name UNIQUE (template_name),
    CONSTRAINT ck_email_template_status
        CHECK (status IN ('DRAFT','PENDING_APPROVAL','ACTIVE','INACTIVE','REJECTED'))
);

COMMENT ON TABLE txn.email_template IS 'Logical template header; owner + pointer to current/active version. Name must be unique (AC: Template Name must be unique).';

-- =====================================================================
-- email_template_version : maker-checker versioned content (EM-13)
-- One row per draft/submission. current_version_id on the parent
-- points here while it is being edited; a new row is created whenever
-- attachments/body/etc change on an already-approved version.
-- =====================================================================
CREATE TABLE txn.email_template_version (
    template_version_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_id              BIGINT            NOT NULL REFERENCES txn.email_template(template_id),
    version_number           INT               NOT NULL,

    -- Template Details step
    from_identity_id         BIGINT            NOT NULL, -- FK -> txn.sender_identity(sender_identity_id) [EM-1]
    data_source_query_id     BIGINT            NOT NULL, -- FK -> txn.data_source_query(query_id) [EM-19]
    data_source_query_version INT              NOT NULL, -- pinned query version at approval time

    -- Email Content step
    subject                  VARCHAR(255)      NOT NULL,
    body_html                TEXT              NOT NULL,  -- WYSIWYG rich-text body, sanitized to supported tag set
    body_plain_text          TEXT              NOT NULL,  -- auto-derived plain-text alternative
    body_size_bytes          INT               NOT NULL,

    -- Recipient mapping (AC: explicit choices, not assumed from column names)
    recipient_mode           VARCHAR(30)       NOT NULL,
    recipient_email_column   VARCHAR(100)      NOT NULL,
    recipient_name_column    VARCHAR(100)      NULL,      -- required only for QUERY_DRIVEN w/ personalized greeting

    -- Maker-checker lifecycle
    status                   VARCHAR(20)       NOT NULL DEFAULT 'DRAFT',
    submitted_by              VARCHAR(100)      NULL,
    submitted_at              TIMESTAMPTZ       NULL,
    approved_by               VARCHAR(100)      NULL,
    approved_at               TIMESTAMPTZ       NULL,
    approval_comments        VARCHAR(500)      NULL,
    rejection_reason         VARCHAR(500)      NULL,

    -- Query-version drift flag (AC: cannot submit until fixed)
    has_merge_field_conflict BOOLEAN           NOT NULL DEFAULT FALSE,
    newer_query_version_available INT          NULL,

    created_by                VARCHAR(100)      NOT NULL,
    created_at                TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_by                VARCHAR(100)      NOT NULL,
    updated_at                TIMESTAMPTZ       NOT NULL DEFAULT now(),

    CONSTRAINT uq_template_version UNIQUE (template_id, version_number),
    CONSTRAINT ck_template_version_status
        CHECK (status IN ('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED')),
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
-- email_template_attachment : stored WITH the version (AC: attachments
-- cannot change on an already-approved version; a new version is
-- created instead). Virus-scanned before persistence.
-- =====================================================================
CREATE TABLE txn.email_template_attachment (
    attachment_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_version_id      BIGINT            NOT NULL REFERENCES txn.email_template_version(template_version_id),
    file_name                VARCHAR(255)      NOT NULL,
    file_extension            VARCHAR(10)       NOT NULL,
    file_size_bytes           BIGINT            NOT NULL,
    storage_path              VARCHAR(500)      NOT NULL, -- blob/object store reference, not raw bytes in DB
    virus_scan_status         VARCHAR(20)       NOT NULL DEFAULT 'PENDING',
    uploaded_by                VARCHAR(100)      NOT NULL,
    uploaded_at                TIMESTAMPTZ       NOT NULL DEFAULT now(),

    CONSTRAINT ck_attachment_scan_status
        CHECK (virus_scan_status IN ('PENDING','CLEAN','INFECTED','FAILED')),
    CONSTRAINT ck_attachment_size CHECK (file_size_bytes <= 10485760) -- 10MB per screen
);

CREATE INDEX ix_attachment_version_id ON txn.email_template_attachment(template_version_id);

-- =====================================================================
-- email_template_merge_field : every placeholder inserted into subject
-- or body, tied to a real query column at insert time (AC: picked from
-- menu, never hand-typed). Flags when the backing column disappears.
-- =====================================================================
CREATE TABLE txn.email_template_merge_field (
    merge_field_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_version_id       BIGINT            NOT NULL REFERENCES txn.email_template_version(template_version_id),
    placeholder_name          VARCHAR(100)      NOT NULL,  -- e.g. "full_name" (rendered as {{full_name}})
    source_column             VARCHAR(100)      NOT NULL,  -- query column it maps to
    field_location             VARCHAR(10)       NOT NULL,  -- SUBJECT | BODY
    is_broken                  BOOLEAN           NOT NULL DEFAULT FALSE, -- true if source_column no longer in query

    CONSTRAINT ck_merge_field_location CHECK (field_location IN ('SUBJECT','BODY'))
);

CREATE INDEX ix_merge_field_version_id ON txn.email_template_merge_field(template_version_id);

-- =====================================================================
-- email_template_recipient_selection : chosen distribution lists /
-- contacts when recipient_mode = CONTACT_DISTRIBUTION_LIST.
-- Assumes txn.distribution_list and txn.contact already exist
-- (referenced generically here; adjust FK targets to actual tables).
-- =====================================================================
CREATE TABLE txn.email_template_recipient_selection (
    selection_id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_version_id        BIGINT            NOT NULL REFERENCES txn.email_template_version(template_version_id),
    distribution_list_id       BIGINT            NULL,   -- FK -> txn.distribution_list
    contact_id                  BIGINT            NULL,   -- FK -> txn.contact
    added_at                    TIMESTAMPTZ       NOT NULL DEFAULT now(),

    CONSTRAINT ck_recipient_selection_one_of
        CHECK (
            (distribution_list_id IS NOT NULL AND contact_id IS NULL) OR
            (distribution_list_id IS NULL AND contact_id IS NOT NULL)
        )
);

CREATE INDEX ix_recipient_selection_version_id ON txn.email_template_recipient_selection(template_version_id);

-- =====================================================================
-- email_template_audit_log : full audit trail, matching EM-1's
-- "full audit logging" convention.
-- =====================================================================
CREATE TABLE txn.email_template_audit_log (
    audit_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_id         BIGINT            NOT NULL REFERENCES txn.email_template(template_id),
    template_version_id BIGINT            NULL REFERENCES txn.email_template_version(template_version_id),
    action              VARCHAR(50)       NOT NULL, -- CREATED, SUBMITTED, APPROVED, REJECTED, ATTACHMENT_ADDED, OWNER_REASSIGNED, ...
    performed_by         VARCHAR(100)      NOT NULL,
    performed_at         TIMESTAMPTZ       NOT NULL DEFAULT now(),
    detail               VARCHAR(1000)     NULL
);

CREATE INDEX ix_audit_log_template_id ON txn.email_template_audit_log(template_id);
