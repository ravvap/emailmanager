package com.fdic.tip.emailmanager.template.enums;

/**
 * Lifecycle status of an individual email_template_version (EM-11 AC:
 * "Every version (draft, pending, active, superseded, rejected,
 * withdrawn) is preserved with its full content and lifecycle details").
 */
public enum VersionStatus {
    DRAFT,
    PENDING_APPROVAL,
    ACTIVE,
    SUPERSEDED,
    REJECTED,
    WITHDRAWN
}
