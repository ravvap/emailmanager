package com.fdic.tip.emailmanager.template.entity;

import com.fdic.tip.emailmanager.template.enums.ChangeRequestStatus;
import com.fdic.tip.emailmanager.template.enums.ChangeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * The single maker-checker request covering every template change type
 * (EM-13): NEW_TEMPLATE, EDIT, RETIRE, REACTIVATE, RESTORE. An approver
 * must be someone other than the submitter — enforced both here and at
 * the DB level (ck_change_request_no_self_approval).
 */
@Entity
@Table(name = "email_template_change_request", schema = "txn")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplateChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "change_request_id")
    private Long changeRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private EmailTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 20, nullable = false)
    private ChangeType changeType;

    // Populated for NEW_TEMPLATE / EDIT / RESTORE; null for RETIRE / REACTIVATE
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_version_id")
    private EmailTemplateVersion templateVersion;

    // Populated only for RESTORE
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restore_from_version_id")
    private EmailTemplateVersion restoreFromVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ChangeRequestStatus status;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "submitted_by", length = 100, nullable = false)
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "decided_by", length = 100)
    private String decidedBy;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "decision_comments", length = 500)
    private String decisionComments;

    @PrePersist
    protected void onCreate() {
        this.submittedAt = OffsetDateTime.now();
        if (this.status == null) {
            this.status = ChangeRequestStatus.PENDING;
        }
    }
}
