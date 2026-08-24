package com.fdic.tip.emailmanager.constant;

/**
 * Field length limits. Kept in sync with the Add/Edit Data Connection modal
 * ("Max 255 characters" on Description in the UX design) and column widths in the DDL.
 */
public final class FieldLimits {

    private FieldLimits() {
    }

    public static final int NAME_MAX_LENGTH = 255;
    public static final int DESCRIPTION_MAX_LENGTH = 255;
    public static final int DATABASE_LOCATION_MAX_LENGTH = 500;
    public static final int VAULT_CREDENTIAL_MAX_LENGTH = 255;
    public static final int USERNAME_MAX_LENGTH = 100;
    public static final int AUDIT_DETAILS_MAX_LENGTH = 4000;

    public static final int DEFAULT_PAGE_SIZE = 25;
    public static final int MAX_PAGE_SIZE = 200;
}
