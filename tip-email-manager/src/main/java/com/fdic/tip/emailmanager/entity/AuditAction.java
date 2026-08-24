package com.fdic.tip.emailmanager.entity;

/**
 * Every create, edit, and status change on a data connection is recorded in the audit log
 * (EM-1 acceptance criteria). Test attempts and author grants/revokes are also captured
 * for traceability since they touch access to secured connections.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    STATUS_CHANGE,
    DELETE,
    TEST_SUCCESS,
    TEST_FAILURE,
    AUTHOR_GRANTED,
    AUTHOR_REVOKED
}
