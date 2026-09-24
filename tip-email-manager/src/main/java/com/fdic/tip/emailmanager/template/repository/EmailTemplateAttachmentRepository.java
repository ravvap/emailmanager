package com.fdic.tip.emailmanager.template.repository;

import com.fdic.tip.emailmanager.template.entity.EmailTemplateAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailTemplateAttachmentRepository extends JpaRepository<EmailTemplateAttachment, Long> {

    List<EmailTemplateAttachment> findByTemplateVersion_TemplateVersionId(Long templateVersionId);
}
