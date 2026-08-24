package com.fdic.tip.emailmanager.service.impl;

import com.fdic.tip.emailmanager.service.AzureMailboxConfigService;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AzureMailboxConfigServiceImpl implements AzureMailboxConfigService {

    private final GraphServiceClient graphServiceClient;

    @Override
    public void configureAzureMailbox(String emailAddress) {
        log.info("Configuring Azure Cloud Mailbox settings for: {}", emailAddress);
        try {
            // Check if user exists in Azure AD
            User user = graphServiceClient.users(emailAddress)
                    .buildRequest()
                    .get();

            if (user == null) {
                throw new IllegalStateException("User mailbox " + emailAddress + " does not exist in Azure AD.");
            }

            // Verify account status and update user attributes if necessary
            User updatedUser = new User();
            updatedUser.accountEnabled = true;

            graphServiceClient.users(emailAddress)
                    .buildRequest()
                    .patch(updatedUser);

            log.info("Successfully provisioned and verified Azure mailbox configuration for {}", emailAddress);
        } catch (Exception e) {
            log.error("Failed to configure Azure Mailbox for {}: {}", emailAddress, e.getMessage(), e);
            throw new IllegalStateException("Azure Mailbox configuration failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void disableAzureMailbox(String emailAddress) {
        log.info("Disabling Azure Cloud Mailbox settings for: {}", emailAddress);
        try {
            User updatedUser = new User();
            updatedUser.accountEnabled = false;

            graphServiceClient.users(emailAddress)
                    .buildRequest()
                    .patch(updatedUser);

            log.info("Successfully disabled Azure mailbox for {}", emailAddress);
        } catch (Exception e) {
            log.error("Failed to disable Azure Mailbox for {}: {}", emailAddress, e.getMessage(), e);
        }
    }
}