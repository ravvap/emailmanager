package gov.fdic.tip.emailmanager.entity;

/**
 * Status values representing the lifecycle states of a Data Source Query version.
 *
 * @author prasad ravva
 */
public enum QueryStatus {
    PENDING_REVIEW,
    ACTIVE,
    REJECTED,
    RETIRED
}