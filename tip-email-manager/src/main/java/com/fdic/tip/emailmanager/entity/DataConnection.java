package com.fdic.tip.emailmanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.Set;

@Entity
@Table(name = "data_connection")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    // Fix: Map as DatabaseLocation entity, not String
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_location_id", nullable = false)
    private DatabaseLocation databaseLocation;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "is_used_in_template", nullable = false)
    private boolean isUsedInTemplate;

    @ElementCollection
    @CollectionTable(name = "data_connection_authors", joinColumns = @JoinColumn(name = "data_connection_id"))
    @Column(name = "author_user_id")
    private Set<Long> authorUserIds;

    @Column(name = "created_by", length = 100, nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;
}