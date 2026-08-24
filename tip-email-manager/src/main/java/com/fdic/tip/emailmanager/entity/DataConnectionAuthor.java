package com.fdic.tip.emailmanager.entity;

import com.fdic.tip.emailmanager.constant.FieldLimits;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Grants a single author (template builder) permission to use a given data connection.
 * Optional at creation time (per the Add Data Connection modal: "Authors" is not marked
 * required), multi-select in the UI. Sensitive sources are only usable by granted authors.
 */
@Entity
@Table(
        name = "tip_data_connection_author",
        schema = "txn",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_data_conn_author",
                columnNames = {"data_connection_id", "author_username"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "dataConnection")
public class DataConnectionAuthor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_connection_id", nullable = false)
    private DataConnection dataConnection;

    @Column(name = "author_username", nullable = false, length = FieldLimits.USERNAME_MAX_LENGTH)
    private String authorUsername;

    @Column(name = "granted_by", nullable = false, length = FieldLimits.USERNAME_MAX_LENGTH)
    private String grantedBy;

    @CreationTimestamp
    @Column(name = "granted_date", nullable = false, updatable = false)
    private Instant grantedDate;
}
