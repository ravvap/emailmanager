package com.fdic.tip.emailmanager.repository;

import com.fdic.tip.emailmanager.entity.InternalDomainAllowlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternalDomainAllowlistRepository extends JpaRepository<InternalDomainAllowlist, Long> {

    Optional<InternalDomainAllowlist> findByIdAndDeletedAtIsNull(Long id);

    List<InternalDomainAllowlist> findByDeletedAtIsNull();

    boolean existsByDomainAndDeletedAtIsNull(String domain);

    boolean existsByDomainAndIdNotAndDeletedAtIsNull(String domain, Long id);
}