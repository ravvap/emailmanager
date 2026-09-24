package com.fdic.tip.emailmanager.template.adapter;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.fdic.tip.emailmanager.common.constants.AttachmentStorageConstants;
import com.fdic.tip.emailmanager.template.service.AttachmentStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Stores template attachments in Azure Blob Storage, gated by a virus
 * scan (EM-8 AC: "attachments... are virus-scanned"). Scanning and
 * storage are two collaborators, not one god class — VirusScanClient is
 * a separate single-responsibility component so the scan step can be
 * swapped independently of where files end up.
 *
 * Maven: com.azure:azure-storage-blob (uses the platform's existing
 * Azure Entra ID / Managed Identity credential — no separate connection
 * string secret, consistent with the rest of TIP's Azure integrations).
 */
@Component
public class AttachmentStoragePortImpl implements AttachmentStoragePort {

    private final BlobContainerClient containerClient;
    private final VirusScanClient virusScanClient;

    public AttachmentStoragePortImpl(
            BlobServiceClient blobServiceClient,
            VirusScanClient virusScanClient,
            @Value("${" + AttachmentStorageConstants.PROP_CONTAINER_NAME + ":"
                    + AttachmentStorageConstants.DEFAULT_CONTAINER_NAME + "}") String containerName) {
        this.virusScanClient = virusScanClient;
        this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!this.containerClient.exists()) {
            this.containerClient.create();
        }
    }

    @Override
    public String storeAndScan(MultipartFile file) {
        byte[] content = readBytes(file);
        if (content.length == 0) {
            throw new IllegalArgumentException(AttachmentStorageConstants.MSG_EMPTY_FILE);
        }

        VirusScanResult scanResult = virusScanClient.scan(content, file.getOriginalFilename());
        if (scanResult.verdict() == VirusScanResult.Verdict.INFECTED) {
            throw new SecurityException(AttachmentStorageConstants.MSG_SCAN_INFECTED);
        }
        if (scanResult.verdict() != VirusScanResult.Verdict.CLEAN) {
            throw new IllegalStateException(AttachmentStorageConstants.MSG_SCAN_FAILED);
        }

        String blobName = buildBlobName(file.getOriginalFilename());
        try {
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.upload(new ByteArrayInputStream(content), content.length, true);
            return containerClient.getBlobContainerName() + "/" + blobName;
        } catch (RuntimeException ex) {
            throw new IllegalStateException(AttachmentStorageConstants.MSG_UPLOAD_FAILED, ex);
        }
    }

    private String buildBlobName(String originalFileName) {
        String safeName = originalFileName == null ? "attachment" : originalFileName.replaceAll("[^A-Za-z0-9._-]", "_");
        return AttachmentStorageConstants.BLOB_PATH_PREFIX + "/" + UUID.randomUUID() + "-" + safeName;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException(AttachmentStorageConstants.MSG_UPLOAD_FAILED, ex);
        }
    }
}
