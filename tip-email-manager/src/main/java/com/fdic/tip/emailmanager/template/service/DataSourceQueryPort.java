package com.fdic.tip.emailmanager.template.service;

import java.util.List;
import java.util.UUID;

/**
 * Boundary into the EM-19 Data Source Query catalog (the
 * {@code data_source_query} table — asset_id groups a query's versions,
 * each version is its own row with its own row id, status
 * PENDING_REVIEW/ACTIVE/REJECTED/RETIRED).
 */
public interface DataSourceQueryPort {

    boolean isAuthorized(String userId, UUID dataSourceQueryId);

    /** Returns the version number of the latest ACTIVE row for this asset_id. */
    int getApprovedVersion(UUID dataSourceQueryId);

    /** Returns the result columns for the (asset_id, version) row, via the platform's existing columns endpoint. */
    List<String> getColumns(UUID dataSourceQueryId, int queryVersion);
}
