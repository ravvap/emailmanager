package com.fdic.tip.emailmanager.template.repository;

import com.fdic.tip.emailmanager.template.entity.EmailTemplateRecipientSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailTemplateRecipientSelectionRepository extends JpaRepository<EmailTemplateRecipientSelection, Long> {

    List<EmailTemplateRecipientSelection> findByTemplateVersion_TemplateVersionId(Long templateVersionId);

    void deleteByTemplateVersion_TemplateVersionId(Long templateVersionId);
}
