package com.fdic.tip.emailmanager.template.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Boundary for storing/retrieving the uploaded recipient file
 * (FILE_UPLOAD recipient mode). Kept separate from AttachmentStoragePort
 * even though both end up in blob storage behind the same virus-scan
 * gate — a recipient file isn't an email attachment, it's recipient
 * data, and the Preview screen displays them in different sections.
 */
public interface RecipientFileStoragePort {

    /** Stores and virus-scans the file, returning its storage path. */
    String storeAndScan(MultipartFile file);

    /** Retrieves the stored file's bytes for sheet/column parsing. */
    byte[] fetch(String storagePath);
}
