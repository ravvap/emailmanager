package com.fdic.tip.emailmanager.entity;

/**
 * Status of a registered data connection. Only ACTIVE connections are selectable
 * when authoring templates (see EM-1 acceptance criteria).
 */
public enum ConnectionStatus {
    ACTIVE,
    INACTIVE
}
