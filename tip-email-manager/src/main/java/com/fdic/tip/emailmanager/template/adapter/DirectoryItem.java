package com.fdic.tip.emailmanager.template.adapter;

/**
 * Minimal shape expected back from the existing distribution-list /
 * contacts "get" endpoints — just enough to validate a selection (id +
 * active status). Field names/casing are this adapter's best guess;
 * reconcile against the endpoints' actual response shape (e.g. via
 * @JsonProperty if the real field names differ) before wiring this up.
 */
public record DirectoryItem(Long id, String name, String status) {

    public boolean isActive() {
        return status != null && "active".equalsIgnoreCase(status);
    }
}
