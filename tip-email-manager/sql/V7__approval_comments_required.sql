-- =====================================================================
-- TIP Email Manager — Email Template module, V7
-- Preview-and-Submit screen's "Approval Comments" field is required
-- when submitting a NEW_TEMPLATE or EDIT change request — enforced at
-- the DB level too, same pattern as ck_change_request_no_self_approval
-- (V2): the service-layer check is the primary gate (clearer error
-- message), this is the backstop. RETIRE/REACTIVATE keep their reason
-- optional per EM-10 AC; RESTORE doesn't collect one at all.
-- =====================================================================

ALTER TABLE txn.email_template_change_request
    ADD CONSTRAINT ck_change_request_approval_comments_required
        CHECK (
            change_type NOT IN ('NEW_TEMPLATE','EDIT')
            OR (reason IS NOT NULL AND length(trim(reason)) > 0)
        );
