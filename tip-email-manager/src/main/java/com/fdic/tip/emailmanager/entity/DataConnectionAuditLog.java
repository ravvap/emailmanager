package com.fdic.tip.emailmanager.entity;

import com.fdic.tip.emailmanager.constant.FieldLimits;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit trail entry. Deliberately NOT foreign-keyed with ON DELETE CASCADE to
 * the parent connection: once a connection is permanently deleted (only allowed when it
 * has no template usage history) its audit trail must still be retrievable, so the
 * connection name is snapshotted here and the FK is nullable / ON DELETE SET NULL.
 */
@Entity
@Table(name = "tip_data_connection_audit_log", schema = "txn")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataConnectionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "data_connection_id")
    private UUID dataConnectionId;

    @Column(name = "data_connection_name", nullable = false, length = FieldLimits.NAME_MAX_LENGTH)
    private String dataConnectionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private AuditAction action;

    @Column(name = "performed_by", nullable = false, length = FieldLimits.USERNAME_MAX_LENGTH)
    private String performedBy;

    @CreationTimestamp
    @Column(name = "performed_date", nullable = false, updatable = false)
    private Instant performedDate;

    @Column(name = "details", length = FieldLimits.AUDIT_DETAILS_MAX_LENGTH)
    private String details;
}
