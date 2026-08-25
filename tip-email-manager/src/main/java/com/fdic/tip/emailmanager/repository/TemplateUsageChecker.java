package com.fdic.tip.emailmanager.repository;

/**
 * Backs the "connection has never been used by any template" delete guard.
 *
 * connection.isUsedInTemplate() in the original code had no writer anywhere - it would
 * always evaluate false, silently defeating the guard. Since templates live in their own
 * table (per your note), this needs to be a real query against whatever join/FK the
 * Template module ends up creating (e.g. a template_data_connection table, or a
 * data_connection_id FK column directly on the template table).
 *
 * Until that module exists, TemplateUsageChecker below is a stub that always returns
 * false (nothing has used a connection yet, so nothing blocks delete) - replace its
 * implementation with a real @Repository once the Template table exists. Do NOT ship
 * the stub to production as-is: it makes the delete guard a no-op the same way the
 * original isUsedInTemplate() was.
 */
public interface TemplateUsageChecker {

    boolean isConnectionUsedByAnyTemplate(Long dataConnectionId);
}
