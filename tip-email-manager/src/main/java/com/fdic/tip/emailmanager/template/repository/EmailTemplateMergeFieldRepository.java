package com.fdic.tip.emailmanager.template.repository;

import com.fdic.tip.emailmanager.template.entity.EmailTemplateMergeField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailTemplateMergeFieldRepository extends JpaRepository<EmailTemplateMergeField, Long> {

    List<EmailTemplateMergeField> findByTemplateVersion_TemplateVersionId(Long templateVersionId);

    List<EmailTemplateMergeField> findByTemplateVersion_TemplateVersionIdAndIsBrokenTrue(Long templateVersionId);
}
