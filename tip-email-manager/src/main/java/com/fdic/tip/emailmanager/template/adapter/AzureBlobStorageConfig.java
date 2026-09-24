package com.fdic.tip.emailmanager.template.adapter;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Blob Storage client for template attachments, authenticated via
 * DefaultAzureCredential (Managed Identity in Azure, developer
 * credentials locally) — no storage account key/connection-string
 * secret to manage, consistent with the rest of TIP's Azure
 * integrations.
 */
@Configuration
public class AzureBlobStorageConfig {

    @Bean
    public BlobServiceClient blobServiceClient(@Value("${tip.email-manager.attachments.blob-endpoint}") String blobEndpoint) {
        return new BlobServiceClientBuilder()
                .endpoint(blobEndpoint)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }
}
