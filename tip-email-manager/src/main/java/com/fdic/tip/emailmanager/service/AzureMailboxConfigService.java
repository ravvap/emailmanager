package com.fdic.tip.emailmanager.service;

public interface AzureMailboxConfigService {

    /**
     * Provisions or updates a No-Reply mailbox inside Azure AD / Exchange Online.
     *
     * @param emailAddress The email address to configure in Azure.
     */
    void configureAzureMailbox(String emailAddress);

    /**
     * Deactivates or removes mailbox bindings in Azure Cloud.
     *
     * @param emailAddress The target email address to disable in Azure.
     */
    void disableAzureMailbox(String emailAddress);
}