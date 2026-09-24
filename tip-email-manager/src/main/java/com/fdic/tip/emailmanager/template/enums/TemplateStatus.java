package com.fdic.tip.emailmanager.template.enums;

/**
 * Status of the logical email_template header row.
 * V2: PENDING_APPROVAL/INACTIVE/REJECTED removed — a pending change or a
 * rejected draft is now reflected on the version / change-request rows,
 * not here, so the template can't hold two contradictory states at once.
 */
public enum TemplateStatus {
    DRAFT,
    ACTIVE,
    RETIRED
}
