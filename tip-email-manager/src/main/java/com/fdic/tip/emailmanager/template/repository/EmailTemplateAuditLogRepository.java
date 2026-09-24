package com.fdic.tip.emailmanager.template.repository;

import com.fdic.tip.emailmanager.template.entity.EmailTemplateAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailTemplateAuditLogRepository extends JpaRepository<EmailTemplateAuditLog, Long> {

    List<EmailTemplateAuditLog> findByTemplateIdOrderByPerformedAtDesc(Long templateId);
}
