package com.fdic.tip.emailmanager.common.constants;

/**
 * Table name, status values, and config keys for the EM-19 Data Source
 * Query adapter — matches the actual data_source_query table (plain
 * public schema, no FK to permissions: asset_id groups a query's
 * versions, each version is its own row with status
 * PENDING_REVIEW/ACTIVE/REJECTED/RETIRED).
 */
public final class DataSourceQueryConstants {

    private DataSourceQueryConstants() {
    }

    public static final String TABLE_QUERY = "data_source_query";

    public static final String STATUS_ACTIVE = "ACTIVE";

    // application.yml key — base URL of the platform's existing
    // "get data source query columns" endpoint. This module appends
    // /{queryRowId}/columns to it (see DataSourceQueryPortImpl).
    public static final String PROP_COLUMNS_ENDPOINT_BASE_URL = "tip.email-manager.data-source-query.columns-endpoint-base-url";

    public static final String MSG_QUERY_NOT_FOUND = "Data source query not found.";
    public static final String MSG_NO_APPROVED_VERSION = "This data source query has no active (approved) version.";
    public static final String MSG_VERSION_NOT_FOUND = "That data source query version was not found.";
    public static final String MSG_COLUMNS_FETCH_FAILED = "Could not retrieve columns for this data source query.";
}
