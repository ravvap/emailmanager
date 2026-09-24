package com.fdic.tip.emailmanager.template.service;

import org.springframework.web.multipart.MultipartFile;

/** Boundary into blob storage + virus scanning for template attachments. */
public interface AttachmentStoragePort {

    /** Stores the file and virus-scans it; throws if infected/scan fails. Returns the storage path/reference. */
    String storeAndScan(MultipartFile file);
}
