package com.fdic.tip.emailmanager.template.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * A chosen distribution list or individual contact for a version whose
 * recipientMode is CONTACT_DISTRIBUTION_LIST. Exactly one of
 * distributionListId / contactId is set per row.
 */
@Entity
@Table(name = "email_template_recipient_selection", schema = "txn")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplateRecipientSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "selection_id")
    private Long selectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_version_id", nullable = false)
    private EmailTemplateVersion templateVersion;

    @Column(name = "distribution_list_id")
    private Long distributionListId;

    @Column(name = "contact_id")
    private Long contactId;

    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        this.addedAt = OffsetDateTime.now();
    }
}
