package com.fdic.tip.emailmanager.template.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/** Full audit trail entry — matches the EM-1 "full audit logging" convention. */
@Entity
@Table(name = "email_template_audit_log", schema = "txn")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplateAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "template_version_id")
    private Long templateVersionId;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "performed_by", length = 100, nullable = false)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private OffsetDateTime performedAt;

    @Column(name = "detail", length = 1000)
    private String detail;

    @PrePersist
    protected void onCreate() {
        this.performedAt = OffsetDateTime.now();
    }
}
