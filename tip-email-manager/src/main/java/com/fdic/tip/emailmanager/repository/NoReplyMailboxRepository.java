package com.fdic.tip.emailmanager.repository;

import com.fdic.tip.emailmanager.entity.NoReplyMailbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoReplyMailboxRepository extends JpaRepository<NoReplyMailbox, Long> {

    Optional<NoReplyMailbox> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByEmailAddressAndIdNotAndDeletedAtIsNull(String emailAddress, Long id);
}