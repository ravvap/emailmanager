package com.fdic.tip.emailmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "app_role")
@Getter
@Setter
public class AppRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "name", length = 150)
    private String name;

    @Version
    private Long version;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "idp_role_name", length = 150)
    private String idpRoleName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder;
}