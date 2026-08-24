package com.fdic.tip.emailmanager.entity;

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
 * Generic usage ledger: any record here means the connection has been referenced by a
 * template (or other consumer) and therefore has history, which blocks permanent delete
 * per EM-1 acceptance criteria ("a connection that has never been used by any template
 * can be permanently deleted; one that has history cannot").
 *
 * The Email Manager Template module (a later story, not yet built) is expected to insert
 * a row here the first time a template references a connection. Keeping this as a
 * standalone table -- rather than a FK straight to a not-yet-existing template table --
 * lets this module ship independently and lets the template module wire into it later
 * without a schema change here.
 */
@Entity
@Table(name = "tip_data_connection_usage", schema = "txn")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataConnectionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "data_connection_id", nullable = false)
    private UUID dataConnectionId;

    /** e.g. "EMAIL_TEMPLATE" - kept as a free-form discriminator for future consumer types. */
    @Column(name = "referenced_by_type", nullable = false, length = 50)
    private String referencedByType;

    @Column(name = "referenced_by_id", nullable = false, length = 100)
    private String referencedById;

    @CreationTimestamp
    @Column(name = "referenced_date", nullable = false, updatable = false)
    private Instant referencedDate;
}
