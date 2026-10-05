-- =====================================================================
-- TIP Email Manager — Email Template module, V2
-- Adds: EM-9 (Edit), EM-10 (Retire/Reactivate), EM-11 (History/Compare),
--       EM-12 (Restore), EM-13 (generalized maker-checker).
--
-- Key change from V1: submission/approval fields move OFF
-- email_template_version and into a new email_template_change_request
-- table. EM-13 makes clear one approval gate covers every change type
-- (new authoring, edit, retire, reactivate, restore) — retire/reactivate
-- have no content version to attach approval fields to, so a
-- version-scoped approval model can't represent them. A separate
-- request table also gives a natural home for the approval queue,
-- withdraw, and the "reviewer sees before/after" comparison.
--
-- Conventions carried over from your V1 EM-1 optimization: partial
-- unique indexes scoped to deleted_at (soft-delete never blocks reuse),
-- LOWER() on case-insensitive text, explicit ON DELETE actions, and
-- created_by/updated_by/deleted_by audit columns everywhere.
-- =====================================================================

-- ---------------------------------------------------------------------
-- email_template.status: DRAFT (never had an approved version) |
-- ACTIVE (has a serving version) | RETIRED. PENDING_APPROVAL/INACTIVE/
-- REJECTED are dropped from here — "is there a pending change" and
-- "was a draft rejected" now live on the version/change-request rows,
-- not as a template-level status, so a template can't be in two
-- contradictory states at once.
-- ---------------------------------------------------------------------
ALTER TABLE txn.email_template
    DROP CONSTRAINT ck_email_template_status;

ALTER TABLE txn.email_template
    ADD CONSTRAINT ck_email_template_status
        CHECK (status IN ('DRAFT','ACTIVE','RETIRED'));

-- ---------------------------------------------------------------------
-- email_template_version: drop the maker-checker columns (moved to
-- email_template_change_request below); widen status to the full
-- lifecycle EM-11 requires; add recipient-mapping conflict flag
-- (mirrors has_merge_field_conflict, but for the designated
-- email/name columns specifically — EM-9 AC) and a self-reference
-- for restore provenance (EM-12: "restored-from" stays visible in
-- history).
-- ---------------------------------------------------------------------
ALTER TABLE txn.email_template_version
    DROP COLUMN submitted_by,
    DROP COLUMN submitted_at,
    DROP COLUMN approved_by,
    DROP COLUMN approved_at,
    DROP COLUMN approval_comments,
    DROP COLUMN rejection_reason;

ALTER TABLE txn.email_template_version
    ADD COLUMN has_recipient_mapping_conflict BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN restored_from_version_id BIGINT
        REFERENCES txn.email_template_version(template_version_id);

ALTER TABLE txn.email_template_version
    DROP CONSTRAINT ck_template_version_status;

ALTER TABLE txn.email_template_version
    ADD CONSTRAINT ck_template_version_status
        CHECK (status IN ('DRAFT','PENDING_APPROVAL','ACTIVE','SUPERSEDED','REJECTED','WITHDRAWN'));

-- ---------------------------------------------------------------------
-- email_template_change_request : the single maker-checker gate over
-- every template change type (EM-13).
-- ---------------------------------------------------------------------
CREATE TABLE txn.email_template_change_request (
    change_request_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_id              BIGINT            NOT NULL REFERENCES txn.email_template(template_id),
    change_type              VARCHAR(20)       NOT NULL,
    template_version_id      BIGINT            NULL REFERENCES txn.email_template_version(template_version_id),
        -- populated for NEW_TEMPLATE / EDIT / RESTORE; NULL for RETIRE / REACTIVATE
    restore_from_version_id  BIGINT            NULL REFERENCES txn.email_template_version(template_version_id),
        -- populated only for RESTORE, for quick "restored from vN" display without joining through the version row

    status                    VARCHAR(20)       NOT NULL DEFAULT 'PENDING',
    reason                    VARCHAR(500)      NULL,   -- submitter's reason (e.g. retire reason); optional per AC
    rejection_reason          VARCHAR(500)      NULL,

    submitted_by               VARCHAR(100)      NOT NULL,
    submitted_at               TIMESTAMPTZ       NOT NULL DEFAULT now(),
    decided_by                 VARCHAR(100)      NULL,
    decided_at                 TIMESTAMPTZ       NULL,
    decision_comments          VARCHAR(500)      NULL,

    CONSTRAINT ck_change_request_type
        CHECK (change_type IN ('NEW_TEMPLATE','EDIT','RETIRE','REACTIVATE','RESTORE')),
    CONSTRAINT ck_change_request_status
        CHECK (status IN ('PENDING','APPROVED','REJECTED','WITHDRAWN')),
    CONSTRAINT ck_change_request_version_required
        CHECK (
            (change_type IN ('NEW_TEMPLATE','EDIT','RESTORE') AND template_version_id IS NOT NULL)
            OR (change_type IN ('RETIRE','REACTIVATE') AND template_version_id IS NULL)
        ),
    -- FIX (self-approval block, EM-13 AC "the system blocks it"): enforced
    -- at the DB level, not just in the service layer — an approver can
    -- never be the same person as the submitter on the same request.
    CONSTRAINT ck_change_request_no_self_approval
        CHECK (decided_by IS NULL OR decided_by <> submitted_by)
);

-- Approval queue lookups: "pending items, optionally filtered by type"
CREATE INDEX idx_change_request_pending_queue
    ON txn.email_template_change_request (status, change_type, submitted_at);

CREATE INDEX idx_change_request_template_id
    ON txn.email_template_change_request (template_id);

-- FIX: enforce "only one draft awaiting approval per template at a
-- time" (EM-9 AC) at the DB level as a belt-and-suspenders check,
-- not just in the service — a partial unique index on (template_id)
-- filtered to PENDING NEW_TEMPLATE/EDIT/RESTORE requests means a
-- second concurrent submission fails fast with a constraint violation
-- instead of a race condition slipping through.
CREATE UNIQUE INDEX uq_one_pending_content_change_per_template
    ON txn.email_template_change_request (template_id)
    WHERE status = 'PENDING' AND change_type IN ('NEW_TEMPLATE','EDIT','RESTORE');

-- Same idea for retire/reactivate — no point queuing two competing
-- lifecycle requests on the same template at once.
CREATE UNIQUE INDEX uq_one_pending_lifecycle_change_per_template
    ON txn.email_template_change_request (template_id)
    WHERE status = 'PENDING' AND change_type IN ('RETIRE','REACTIVATE');

COMMENT ON TABLE txn.email_template_change_request IS
    'Maker-checker request covering every template change type (EM-13): NEW_TEMPLATE, EDIT, RETIRE, REACTIVATE, RESTORE. Approver must differ from submitter (DB-enforced).';
