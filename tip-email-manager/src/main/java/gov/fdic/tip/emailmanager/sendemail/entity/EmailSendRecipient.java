package gov.fdic.tip.emailmanager.sendemail.entity;

import java.time.OffsetDateTime;

import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.BounceCategory;
import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.RecipientClassification;
import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.RecipientDeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "em_email_send_recipient")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSendRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "send_recipient_id")
    private Long sendRecipientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "send_id", nullable = false)
    private EmailSend emailSend;

    @Column(name = "contact_id")
    private Long contactId;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", nullable = false)
    private RecipientClassification classification;

    // Stores resolved per-recipient merge field key/values as JSON
    @Column(name = "merge_values_json", columnDefinition = "TEXT")
    private String mergeValuesJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private RecipientDeliveryStatus deliveryStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "bounce_category", nullable = false)
    private BounceCategory bounceCategory;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "flagged_for_contact_review", nullable = false)
    private boolean flaggedForContactReview;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;
}