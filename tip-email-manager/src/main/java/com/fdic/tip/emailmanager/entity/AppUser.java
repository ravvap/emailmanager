package com.fdic.tip.emailmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idp_subject")
    private String idpSubject;

    @Column(name = "username", nullable = false, length = 150)
    private String username;

    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Version
    private Long version;

    @Column(name = "first_name", length = 32)
    private String firstName;

    @Column(name = "last_name", length = 32)
    private String lastName;

    @Column(name = "status")
    private String status;

    @Column(name = "entra_id")
    private String entraId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<AppRole> roles = new HashSet<>();
}