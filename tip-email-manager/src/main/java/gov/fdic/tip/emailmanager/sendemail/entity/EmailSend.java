package gov.fdic.tip.emailmanager.sendemail.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fdic.tip.emailmanager.template.enums.RecipientMode;

import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.EmailSendStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "em_email_send")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "send_id")
    private Long sendId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "template_version_id", nullable = false)
    private Long templateVersionId;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Column(name = "from_identity_id", nullable = false)
    private Long fromIdentityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_mode", nullable = false)
    private RecipientMode recipientMode;

    // Frozen content snapshot at submission time
    @Column(name = "frozen_subject", nullable = false, length = 1000)
    private String frozenSubject;

    @Column(name = "frozen_body_html", nullable = false, columnDefinition = "TEXT")
    private String frozenBodyHtml;

    @Column(name = "frozen_body_plain_text", columnDefinition = "TEXT")
    private String frozenBodyPlainText;

    // Query metadata for DATA_SOURCE_QUERY dynamic re-run at dispatch
    @Column(name = "data_source_query_id")
    private UUID dataSourceQueryId;
    
    @Column(name = "data_source_query_version")
    private Integer dataSourceQueryVersion;

    @Column(name = "query_parameters_json", columnDefinition = "TEXT")
    private String queryParametersJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmailSendStatus status;

    @Column(name = "total_recipients", nullable = false)
    private int totalRecipients;

    @Column(name = "external_recipients", nullable = false)
    private int externalRecipients;

    @Column(name = "delivered_count", nullable = false)
    private int deliveredCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "submitted_by", nullable = false)
    private String submittedBy;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "review_comments", length = 500)
    private String reviewComments;

    @Column(name = "canceled_at")
    private OffsetDateTime canceledAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "emailSend", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmailSendRecipient> recipients = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "emailSend", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmailSendAttachment> attachments = new ArrayList<>();
}