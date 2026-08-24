package com.fdic.tip.emailmanager.constant;

/**
 * Central home for every user-facing / log message used by the Data Connections module.
 * Keeping these out of the business logic makes copy changes and future localization
 * a one-file exercise instead of a grep-and-replace across services and controllers.
 */
public final class Messages {

    private Messages() {
    }

    // ---- Validation messages ----
    public static final String NAME_REQUIRED = "Name is required.";
    public static final String NAME_TOO_LONG = "Name must not exceed " + FieldLimits.NAME_MAX_LENGTH + " characters.";
    public static final String DESCRIPTION_TOO_LONG =
            "Description must not exceed " + FieldLimits.DESCRIPTION_MAX_LENGTH + " characters.";
    public static final String DATABASE_LOCATION_REQUIRED = "Database location is required.";
    public static final String DATABASE_LOCATION_TOO_LONG =
            "Database location must not exceed " + FieldLimits.DATABASE_LOCATION_MAX_LENGTH + " characters.";
    public static final String VAULT_CREDENTIAL_REQUIRED = "Vault credential reference is required.";
    public static final String VAULT_CREDENTIAL_TOO_LONG =
            "Vault credential reference must not exceed " + FieldLimits.VAULT_CREDENTIAL_MAX_LENGTH + " characters.";
    public static final String STATUS_REQUIRED = "Status is required.";
    public static final String AUTHOR_USERNAME_REQUIRED = "Author username is required.";

    // ---- Business rule / conflict messages ----
    public static final String NAME_ALREADY_EXISTS = "Data Connection Name already exists.";
    public static final String CONNECTION_NOT_FOUND = "Data connection was not found.";
    public static final String CONNECTION_IN_USE_CANNOT_DELETE =
            "This connection has been used by one or more templates and cannot be permanently deleted. "
                    + "Set it to Inactive instead.";
    public static final String CONNECTION_MUST_BE_INACTIVE_TO_DELETE =
            "Only inactive connections can be deleted. Set the connection to Inactive first.";
    public static final String CONNECTION_NOT_ACTIVE_FOR_AUTHORING =
            "This connection is not active and cannot be selected for template authoring.";
    public static final String AUTHOR_NOT_AUTHORIZED_FOR_CONNECTION =
            "You are not an authorized author for this data connection.";
    public static final String OPTIMISTIC_LOCK_CONFLICT =
            "This connection was updated by another user. Please refresh and try again.";

    // ---- Test connection messages ----
    public static final String TEST_CONNECTION_SUCCESS = "Connection test succeeded.";
    // Deliberately generic per acceptance criteria: never expose driver/DB internals to the client.
    public static final String TEST_CONNECTION_FAILURE =
            "Unable to connect using the details provided. Verify the database location and credential, then try again.";
    public static final String TEST_CONNECTION_TIMEOUT =
            "The connection attempt timed out. Verify the database location and try again.";

    // ---- Success / audit messages ----
    public static final String CONNECTION_CREATED = "Data connection created successfully.";
    public static final String CONNECTION_UPDATED = "Data connection updated successfully.";
    public static final String CONNECTION_STATUS_CHANGED = "Data connection status updated successfully.";
    public static final String CONNECTION_DELETED = "Data connection deleted successfully.";
    public static final String AUTHOR_GRANTED = "Author access granted.";
    public static final String AUTHOR_REVOKED = "Author access revoked.";

    // ---- Generic / security messages ----
    public static final String ACCESS_DENIED = "You do not have permission to perform this action.";
    public static final String UNEXPECTED_ERROR = "An unexpected error occurred. Please try again or contact support.";
}
