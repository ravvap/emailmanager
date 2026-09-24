package com.fdic.tip.emailmanager.template.entity;

import com.fdic.tip.emailmanager.template.enums.TemplateStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Logical email template header. Owns a pointer to the version currently
 * being edited (current_version_id) and the last approved, sendable
 * version (active_version_id). Content itself lives in EmailTemplateVersion.
 */
@Entity
@Table(name = "email_template", schema = "txn",
        uniqueConstraints = @UniqueConstraint(name = "uq_email_template_name", columnNames = "template_name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "template_name", length = 150, nullable = false)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TemplateStatus status;

    @Column(name = "owner_user_id", length = 100, nullable = false)
    private String ownerUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private EmailTemplateVersion currentVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_version_id")
    private EmailTemplateVersion activeVersion;

    @Column(name = "created_by", length = 100, nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_by", length = 100, nullable = false)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = TemplateStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
