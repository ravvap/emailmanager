package com.fdic.tip.emailmanager.template.entity;

import com.fdic.tip.emailmanager.template.enums.RecipientMode;
import com.fdic.tip.emailmanager.template.enums.VersionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A single versioned snapshot of a template's content. Pins to a specific
 * approved data source query version at authoring time (EM-19) so a later
 * edit to the shared query does not silently change this template.
 *
 * V2: submission/approval bookkeeping now lives on
 * {@link EmailTemplateChangeRequest} — this entity only tracks the
 * content itself and its resulting lifecycle status.
 */
@Entity
@Table(name = "email_template_version", schema = "txn",
        uniqueConstraints = @UniqueConstraint(name = "uq_template_version", columnNames = {"template_id", "version_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_version_id")
    private Long templateVersionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private EmailTemplate template;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    // ---- Template Details ----
    @Column(name = "from_identity_id", nullable = false)
    private Long fromIdentityId;

    @Column(name = "data_source_query_id", nullable = false)
    private Long dataSourceQueryId;

    @Column(name = "data_source_query_version", nullable = false)
    private Integer dataSourceQueryVersion;

    // ---- Email Content ----
    @Column(name = "subject", length = 255, nullable = false)
    private String subject;

    @Lob
    @Column(name = "body_html", nullable = false)
    private String bodyHtml;

    @Lob
    @Column(name = "body_plain_text", nullable = false)
    private String bodyPlainText;

    @Column(name = "body_size_bytes", nullable = false)
    private Integer bodySizeBytes;

    // ---- Recipient mapping ----
    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_mode", length = 30, nullable = false)
    private RecipientMode recipientMode;

    @Column(name = "recipient_email_column", length = 100, nullable = false)
    private String recipientEmailColumn;

    @Column(name = "recipient_name_column", length = 100)
    private String recipientNameColumn;

    // ---- Lifecycle ----
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private VersionStatus status;

    // Query-version drift flags (AC: cannot submit until fixed)
    @Column(name = "has_merge_field_conflict", nullable = false)
    private Boolean hasMergeFieldConflict;

    @Column(name = "has_recipient_mapping_conflict", nullable = false)
    private Boolean hasRecipientMappingConflict;

    @Column(name = "newer_query_version_available")
    private Integer newerQueryVersionAvailable;

    // EM-12: when this version was produced by a restore, points at the
    // historical version it was copied from — stays visible in history.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restored_from_version_id")
    private EmailTemplateVersion restoredFromVersion;

    @Column(name = "created_by", length = 100, nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_by", length = 100, nullable = false)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "templateVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EmailTemplateAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "templateVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EmailTemplateMergeField> mergeFields = new ArrayList<>();

    @OneToMany(mappedBy = "templateVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EmailTemplateRecipientSelection> recipientSelections = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = VersionStatus.DRAFT;
        }
        if (this.hasMergeFieldConflict == null) {
            this.hasMergeFieldConflict = false;
        }
        if (this.hasRecipientMappingConflict == null) {
            this.hasRecipientMappingConflict = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
