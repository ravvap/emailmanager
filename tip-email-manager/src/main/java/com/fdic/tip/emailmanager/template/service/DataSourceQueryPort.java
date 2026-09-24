package com.fdic.tip.emailmanager.template.service;

import java.util.List;

/**
 * Boundary into the EM-19 Data Source Query module — kept as a narrow
 * port so this module depends on a contract, not that module's internals.
 */
public interface DataSourceQueryPort {

    boolean isAuthorized(String userId, Long dataSourceQueryId);

    int getApprovedVersion(Long dataSourceQueryId);

    List<String> getColumns(Long dataSourceQueryId, int queryVersion);
}
