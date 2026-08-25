package com.fdic.tip.emailmanager.repository;

import com.fdic.tip.emailmanager.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @Query("SELECT DISTINCT u FROM AppUser u JOIN u.roles r " +
           "WHERE (LOWER(r.code) IN ('system_admin', 'admin') OR LOWER(r.name) IN ('system admin', 'admin')) " +
           "AND u.isActive = true AND u.deletedAt IS NULL")
    List<AppUser> findActiveAdminUsers();
}