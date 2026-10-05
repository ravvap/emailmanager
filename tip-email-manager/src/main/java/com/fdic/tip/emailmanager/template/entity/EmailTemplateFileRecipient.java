package com.fdic.tip.emailmanager.template.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * One parsed recipient row (name + email) out of a FILE_UPLOAD-mode
 * recipient file's mapped sheet. Populated by
 * EmailTemplateServiceImpl#updateRecipients once a valid column mapping
 * is chosen; replaced wholesale on any re-upload, re-sheet-selection, or
 * re-mapping — never partially patched.
 */
@Entity
@Table(name = "email_template_file_recipient", schema = "txn",
        uniqueConstraints = @UniqueConstraint(name = "uq_file_recipient_row", columnNames = {"template_version_id", "row_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplateFileRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_recipient_id")
    private Long fileRecipientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_version_id", nullable = false)
    private EmailTemplateVersion templateVersion;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(name = "is_valid", nullable = false)
    private Boolean isValid;

    @Column(name = "validation_error", length = 255)
    private String validationError;

    @PrePersist
    protected void onCreate() {
        if (this.isValid == null) {
            this.isValid = true;
        }
    }
}
