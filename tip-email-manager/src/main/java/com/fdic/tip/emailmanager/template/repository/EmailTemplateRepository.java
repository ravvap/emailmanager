package com.fdic.tip.emailmanager.template.repository;

import com.fdic.tip.emailmanager.template.entity.EmailTemplate;
import com.fdic.tip.emailmanager.template.enums.TemplateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    // FIX: all lookups now exclude soft-deleted rows — a deleted
    // template's name is free to reuse (matches the DB's partial unique
    // index, uq_email_template_name_active) and the template itself
    // should behave as gone, not just differently-statused.
    boolean existsByTemplateNameIgnoreCaseAndDeletedAtIsNull(String templateName);

    Optional<EmailTemplate> findByTemplateNameIgnoreCaseAndDeletedAtIsNull(String templateName);

    Optional<EmailTemplate> findByTemplateIdAndDeletedAtIsNull(Long templateId);

    Page<EmailTemplate> findByDeletedAtIsNull(Pageable pageable);

    Page<EmailTemplate> findByStatusAndDeletedAtIsNull(TemplateStatus status, Pageable pageable);

    Page<EmailTemplate> findByOwnerUserIdAndDeletedAtIsNull(String ownerUserId, Pageable pageable);
}
