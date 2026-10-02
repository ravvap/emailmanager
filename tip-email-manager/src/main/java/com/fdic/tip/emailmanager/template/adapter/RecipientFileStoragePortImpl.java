package com.fdic.tip.emailmanager.template.adapter;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.fdic.tip.emailmanager.common.constants.AttachmentStorageConstants;
import com.fdic.tip.emailmanager.common.constants.RecipientFileConstants;
import com.fdic.tip.emailmanager.template.service.RecipientFileStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Stores the uploaded FILE_UPLOAD-mode recipient file in the same blob
 * container as email attachments, under its own path prefix, gated by
 * the same VirusScanClient (single-responsibility scanner, shared
 * across both storage adapters rather than duplicated).
 */
@Component
public class RecipientFileStoragePortImpl implements RecipientFileStoragePort {

    private static final String PATH_PREFIX = "recipient-files";

    private final BlobContainerClient containerClient;
    private final VirusScanClient virusScanClient;

    public RecipientFileStoragePortImpl(
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

    @Override
    public byte[] fetch(String storagePath) {
        String blobName = storagePath.substring(storagePath.indexOf('/') + 1);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            containerClient.getBlobClient(blobName).downloadStream(out);
            return out.toByteArray();
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException(AttachmentStorageConstants.MSG_UPLOAD_FAILED, ex);
        }
    }

    private String buildBlobName(String originalFileName) {
        String safeName = originalFileName == null ? "recipients" : originalFileName.replaceAll("[^A-Za-z0-9._-]", "_");
        return PATH_PREFIX + "/" + UUID.randomUUID() + "-" + safeName;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException(AttachmentStorageConstants.MSG_UPLOAD_FAILED, ex);
        }
    }
}
