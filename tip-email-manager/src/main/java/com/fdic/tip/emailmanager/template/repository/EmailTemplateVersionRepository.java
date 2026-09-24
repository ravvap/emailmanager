package com.fdic.tip.emailmanager.template.repository;

import com.fdic.tip.emailmanager.template.entity.EmailTemplateVersion;
import com.fdic.tip.emailmanager.template.enums.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailTemplateVersionRepository extends JpaRepository<EmailTemplateVersion, Long> {

    List<EmailTemplateVersion> findByTemplate_TemplateIdOrderByVersionNumberDesc(Long templateId);

    Optional<EmailTemplateVersion> findTopByTemplate_TemplateIdOrderByVersionNumberDesc(Long templateId);

    List<EmailTemplateVersion> findByStatus(VersionStatus status);

    List<EmailTemplateVersion> findByDataSourceQueryId(Long dataSourceQueryId);

    // EM-9 AC: "only one draft awaiting approval per template at a time"
    List<EmailTemplateVersion> findByTemplate_TemplateIdAndStatusIn(Long templateId, List<VersionStatus> statuses);
}
