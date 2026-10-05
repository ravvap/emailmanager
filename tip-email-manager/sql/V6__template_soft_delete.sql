-- =====================================================================
-- TIP Email Manager — Email Template module, V6
-- FIX: email_template never got deleted_by/deleted_at — every other
-- table in your EM-1 schema (database_location, data_connection,
-- contact, distribution_list, ...) carries these alongside its status
-- column. This brings email_template in line and adds the delete
-- capability itself.
--
-- Delete is distinct from Retire: Retire is a reversible business
-- lifecycle state (maker-checker, EM-10) a template can be reactivated
-- out of. Delete is a removal from active management — only permitted
-- from DRAFT or RETIRED (never ACTIVE, never with a pending change
-- request) — and, like every other soft-deletable table here, is a
-- direct action by the template's owner or an admin, not itself a
-- maker-checker action.
-- =====================================================================

ALTER TABLE txn.email_template
    ADD COLUMN deleted_by VARCHAR(100),
    ADD COLUMN deleted_at TIMESTAMPTZ;

-- FIX: the original flat UNIQUE on template_name permanently blocked
-- reusing a deleted template's name, same class of bug as
-- approved_sender/contact_attribute in your EM-1 schema. Replaced with
-- a partial index scoped to deleted_at, consistent with that pattern.
ALTER TABLE txn.email_template
    DROP CONSTRAINT uq_email_template_name;

CREATE UNIQUE INDEX uq_email_template_name_active
    ON txn.email_template (LOWER(template_name))
    WHERE deleted_at IS NULL;

CREATE INDEX idx_email_template_deleted_at
    ON txn.email_template (deleted_at)
    WHERE deleted_at IS NOT NULL;
