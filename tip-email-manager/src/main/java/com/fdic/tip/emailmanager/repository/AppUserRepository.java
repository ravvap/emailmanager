package com.fdic.tip.emailmanager.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

// TODO: adjust this import to wherever your existing app_user entity actually lives.
import com.fdic.tip.emailmanager.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameIgnoreCase(String username);
}
