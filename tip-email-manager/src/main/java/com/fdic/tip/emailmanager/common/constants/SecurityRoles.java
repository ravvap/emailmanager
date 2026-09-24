package com.fdic.tip.emailmanager.common.constants;

/**
 * Role name constants for the Email Template module. Coarse endpoint
 * access (who can hit an endpoint at all) is wired up in SecurityConfig's
 * SecurityFilterChain URL rules, per team convention, using these same
 * constants — not @PreAuthorize. Fine-grained data scoping that isn't a
 * simple "can/can't hit this URL" split (e.g. Analysts seeing only
 * Active templates, or only the active version in history) is applied
 * in the service layer using the authenticated user's granted
 * authorities, which is why these constants are also referenced there.
 */
public final class SecurityRoles {

    private SecurityRoles() {
    }

    public static final String ANALYST = "ROLE_ANALYST";
    public static final String SR_ANALYST = "ROLE_SR_ANALYST";
    public static final String MANAGER = "ROLE_MANAGER";
    public static final String SYSTEM_ADMIN = "ROLE_SYSTEM_ADMIN";
    public static final String INTERNAL_AUDITOR = "ROLE_INTERNAL_AUDITOR";
    public static final String TIP_ADMINISTRATOR = "ROLE_TIP_ADMINISTRATOR";

    /** Roles with full template-list and version-history visibility (EM-11 AC). Analyst is deliberately excluded. */
    public static final String[] TEMPLATE_HISTORY_ROLES = {
            SR_ANALYST, MANAGER, SYSTEM_ADMIN, INTERNAL_AUDITOR
    };

    /** Roles eligible to withdraw any submitter's pending change request (EM-13 AC). */
    public static final String[] WITHDRAW_ON_BEHALF_ROLES = {
            MANAGER, TIP_ADMINISTRATOR
    };

    /** Roles eligible to approve/reject a change request (EM-13 AC); approver must still differ from submitter. */
    public static final String[] APPROVER_ROLES = {
            TIP_ADMINISTRATOR, SR_ANALYST, MANAGER
    };
}
