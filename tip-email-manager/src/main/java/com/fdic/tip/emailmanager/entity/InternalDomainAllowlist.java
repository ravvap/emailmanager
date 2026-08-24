package com.fdic.tip.emailmanager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "internal_domain_allowlist")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalDomainAllowlist extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String domain;

    @Column(nullable = false)
    private String status;
}