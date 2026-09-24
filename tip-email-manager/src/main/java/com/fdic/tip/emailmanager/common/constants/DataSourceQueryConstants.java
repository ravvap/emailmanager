package com.fdic.tip.emailmanager.common.constants;

/**
 * Table/column names and messages for the EM-19 Data Source Query
 * adapter. NOTE: EM-19 owns its own schema; the names below are this
 * module's best assumption of that schema (plain public-schema tables,
 * soft-delete via deleted_at, uppercase status convention — matching
 * the rest of Email Manager) and should be reconciled against EM-19's
 * actual DDL before this adapter is wired up for real.
 */
public final class DataSourceQueryConstants {

    private DataSourceQueryConstants() {
    }

    public static final String TABLE_QUERY = "data_source_query";
    public static final String TABLE_QUERY_VERSION = "data_source_query_version";
    public static final String TABLE_QUERY_PERMISSION = "data_source_query_permission";

    public static final String STATUS_APPROVED = "APPROVED";
    public static final String PRINCIPAL_TYPE_USER = "USER";
    public static final String PRINCIPAL_TYPE_ROLE = "ROLE";

    public static final String MSG_QUERY_NOT_FOUND = "Data source query not found.";
    public static final String MSG_NO_APPROVED_VERSION = "This data source query has no approved version.";
    public static final String MSG_VERSION_NOT_FOUND = "That data source query version was not found.";
}
