package com.fdic.tip.emailmanager.constant;

public final class ApiPaths {

    private ApiPaths() {
    }

    public static final String API_BASE = "/api/v1/email-manager";
    public static final String DATA_CONNECTIONS = API_BASE + "/data-connections";

    public static final String BY_ID = "/{id}";
    public static final String STATUS = "/{id}/status";
    public static final String TEST = "/test";
    public static final String TEST_BY_ID = "/{id}/test";
    public static final String ACTIVE = "/active";
    public static final String AUTHORS = "/{id}/authors";
    public static final String AUTHOR_BY_USERNAME = "/{id}/authors/{username}";
    public static final String AUDIT_LOG = "/{id}/audit-log";
}
