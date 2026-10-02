package com.fdic.tip.emailmanager.common.constants;

/** Config keys and messages for validating CONTACT_DISTRIBUTION_LIST selections against the existing directory endpoints. */
public final class RecipientDirectoryConstants {

    private RecipientDirectoryConstants() {
    }

    // application.yml keys — base URLs of the existing "get distribution
    // list" / "get contacts" endpoints (the same ones the Recipients
    // Mode screen's list grids already call).
    public static final String PROP_DISTRIBUTION_LISTS_URL = "tip.email-manager.directory.distribution-lists-url";
    public static final String PROP_CONTACTS_URL = "tip.email-manager.directory.contacts-url";

    public static final String STATUS_ACTIVE = "ACTIVE"; // compared case-insensitively — distribution_list still uses legacy 'Active' casing

    public static final String MSG_INVALID_RECIPIENT_SELECTION =
            "One or more selected distribution lists or contacts are invalid or inactive.";
    public static final String MSG_DIRECTORY_FETCH_FAILED = "Could not validate the selected recipients. Please try again.";
}
