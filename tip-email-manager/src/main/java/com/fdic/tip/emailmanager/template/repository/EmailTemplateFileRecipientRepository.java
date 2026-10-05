package com.fdic.tip.emailmanager.template.repository;

import com.fdic.tip.emailmanager.template.entity.EmailTemplateFileRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailTemplateFileRecipientRepository extends JpaRepository<EmailTemplateFileRecipient, Long> {

    List<EmailTemplateFileRecipient> findByTemplateVersion_TemplateVersionIdOrderByRowNumber(Long templateVersionId);

    long countByTemplateVersion_TemplateVersionIdAndIsValidFalse(Long templateVersionId);

    void deleteByTemplateVersion_TemplateVersionId(Long templateVersionId);
}
