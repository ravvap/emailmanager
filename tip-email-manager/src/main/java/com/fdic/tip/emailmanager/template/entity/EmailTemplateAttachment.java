package com.fdic.tip.emailmanager.template.entity;

import com.fdic.tip.emailmanager.template.enums.VirusScanStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Attachment stored with a specific template version. Attachments cannot
 * change on an already-approved version — changing one always creates a
 * new version (see EmailTemplateVersion).
 */
@Entity
@Table(name = "email_template_attachment", schema = "txn")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplateAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_version_id", nullable = false)
    private EmailTemplateVersion templateVersion;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "file_extension", length = 10, nullable = false)
    private String fileExtension;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "storage_path", length = 500, nullable = false)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "virus_scan_status", length = 20, nullable = false)
    private VirusScanStatus virusScanStatus;

    @Column(name = "uploaded_by", length = 100, nullable = false)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = OffsetDateTime.now();
        if (this.virusScanStatus == null) {
            this.virusScanStatus = VirusScanStatus.PENDING;
        }
    }
}
