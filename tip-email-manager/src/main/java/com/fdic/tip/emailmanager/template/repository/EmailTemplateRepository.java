package com.fdic.tip.emailmanager.template.repository;

import com.fdic.tip.emailmanager.template.entity.EmailTemplate;
import com.fdic.tip.emailmanager.template.enums.TemplateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    boolean existsByTemplateNameIgnoreCase(String templateName);

    Optional<EmailTemplate> findByTemplateNameIgnoreCase(String templateName);

    Page<EmailTemplate> findByStatus(TemplateStatus status, Pageable pageable);

    Page<EmailTemplate> findByOwnerUserId(String ownerUserId, Pageable pageable);
}
