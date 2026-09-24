package com.fdic.tip.emailmanager.template.repository;

import com.fdic.tip.emailmanager.template.entity.EmailTemplateChangeRequest;
import com.fdic.tip.emailmanager.template.enums.ChangeRequestStatus;
import com.fdic.tip.emailmanager.template.enums.ChangeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailTemplateChangeRequestRepository extends JpaRepository<EmailTemplateChangeRequest, Long> {

    // Approval queue (EM-13 AC: "filterable by change type")
    Page<EmailTemplateChangeRequest> findByStatus(ChangeRequestStatus status, Pageable pageable);

    Page<EmailTemplateChangeRequest> findByStatusAndChangeType(ChangeRequestStatus status, ChangeType changeType, Pageable pageable);

    // "Only one draft awaiting approval per template at a time" — pre-check
    // before hitting the DB partial unique index (EM-9 AC).
    Optional<EmailTemplateChangeRequest> findByTemplate_TemplateIdAndStatus(Long templateId, ChangeRequestStatus status);

    List<EmailTemplateChangeRequest> findByTemplate_TemplateIdOrderBySubmittedAtDesc(Long templateId);
}
