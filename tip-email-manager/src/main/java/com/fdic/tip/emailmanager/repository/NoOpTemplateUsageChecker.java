package com.fdic.tip.emailmanager.repository;

import org.springframework.stereotype.Component;

/**
 * Placeholder so the app compiles and the delete guard has something to call before the
 * Template module exists. Always returns false, meaning delete is never blocked by usage
 * yet - that is INTENTIONALLY visible/searchable (TODO) so it isn't forgotten once
 * templates start referencing connections.
 */
@Component
public class NoOpTemplateUsageChecker implements TemplateUsageChecker {

    @Override
    public boolean isConnectionUsedByAnyTemplate(Long dataConnectionId) {
        // TODO: replace with a real check once the Template module's schema exists, e.g.:
        // return templateRepository.existsByDataConnectionId(dataConnectionId);
        return false;
    }
}
