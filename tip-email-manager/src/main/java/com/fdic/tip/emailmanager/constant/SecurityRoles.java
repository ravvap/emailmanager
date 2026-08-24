package com.fdic.tip.emailmanager.constant;

/**
 * Role names as they appear in the Azure AD app role / "roles" claim of the incoming JWT.
 * Spring Security is configured to prefix these with "ROLE_" when building granted authorities
 * (see SecurityConfig#jwtAuthenticationConverter), so @PreAuthorize expressions reference the
 * *_AUTHORITY constants (which already include the ROLE_ prefix) while raw role name constants
 * are used anywhere the bare claim value is needed (e.g. matching against configuration).
 */
public final class SecurityRoles {

    private SecurityRoles() {
    }

    // Raw role names as issued in the JWT "roles" claim.
    public static final String TIP_ADMIN = "TIP_ADMIN";
    public static final String EMAIL_MANAGER_ADMIN = "EMAIL_MANAGER_ADMIN";
    public static final String EMAIL_MANAGER_AUTHOR = "EMAIL_MANAGER_AUTHOR";

    // Spring Security authority form (ROLE_ prefix), for use in @PreAuthorize("hasAuthority(...)").
    public static final String AUTHORITY_TIP_ADMIN = "ROLE_" + TIP_ADMIN;
    public static final String AUTHORITY_EMAIL_MANAGER_ADMIN = "ROLE_" + EMAIL_MANAGER_ADMIN;
    public static final String AUTHORITY_EMAIL_MANAGER_AUTHOR = "ROLE_" + EMAIL_MANAGER_AUTHOR;

    /** SpEL expression: only administrators may manage (create/edit/delete/status) connections. */
    public static final String CAN_MANAGE_CONNECTIONS =
            "hasAnyAuthority('" + AUTHORITY_TIP_ADMIN + "', '" + AUTHORITY_EMAIL_MANAGER_ADMIN + "')";

    /** SpEL expression: administrators or authors may read/list connections (authors see only what they're granted). */
    public static final String CAN_VIEW_CONNECTIONS =
            "hasAnyAuthority('" + AUTHORITY_TIP_ADMIN + "', '" + AUTHORITY_EMAIL_MANAGER_ADMIN
                    + "', '" + AUTHORITY_EMAIL_MANAGER_AUTHOR + "')";
}
