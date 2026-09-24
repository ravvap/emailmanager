package com.fdic.tip.emailmanager.template.entity;

import com.fdic.tip.emailmanager.template.enums.MergeFieldLocation;
import jakarta.persistence.*;
import lombok.*;

/**
 * Tracks every merge-field placeholder inserted into a version's subject
 * or body, tied to the real query column it was picked from — never
 * hand-typed. is_broken flips true if the pinned query version later
 * loses that column, which blocks submission (AC: flagged and cannot be
 * submitted until fixed).
 */
@Entity
@Table(name = "email_template_merge_field", schema = "txn")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplateMergeField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merge_field_id")
    private Long mergeFieldId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_version_id", nullable = false)
    private EmailTemplateVersion templateVersion;

    @Column(name = "placeholder_name", length = 100, nullable = false)
    private String placeholderName;

    @Column(name = "source_column", length = 100, nullable = false)
    private String sourceColumn;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_location", length = 10, nullable = false)
    private MergeFieldLocation fieldLocation;

    @Column(name = "is_broken", nullable = false)
    private Boolean isBroken;

    @PrePersist
    protected void onCreate() {
        if (this.isBroken == null) {
            this.isBroken = false;
        }
    }
}
