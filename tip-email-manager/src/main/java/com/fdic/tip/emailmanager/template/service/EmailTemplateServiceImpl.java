package com.fdic.tip.emailmanager.template.service;

import com.fdic.tip.emailmanager.common.constants.EmailTemplateConstants;
import com.fdic.tip.emailmanager.common.constants.SecurityRoles;
import com.fdic.tip.emailmanager.template.dto.*;
import com.fdic.tip.emailmanager.template.entity.*;
import com.fdic.tip.emailmanager.template.enums.*;
import com.fdic.tip.emailmanager.template.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailTemplateServiceImpl implements EmailTemplateService {

    private final EmailTemplateRepository templateRepository;
    private final EmailTemplateVersionRepository versionRepository;
    private final EmailTemplateAttachmentRepository attachmentRepository;
    private final EmailTemplateMergeFieldRepository mergeFieldRepository;
    private final EmailTemplateRecipientSelectionRepository recipientSelectionRepository;
    private final EmailTemplateChangeRequestRepository changeRequestRepository;
    private final EmailTemplateAuditLogRepository auditLogRepository;

    private final DataSourceQueryPort dataSourceQueryPort;
    private final AttachmentStoragePort attachmentStoragePort;
    private final RichTextSanitizerPort richTextSanitizerPort;

    private static final List<VersionStatus> OPEN_VERSION_STATUSES = List.of(VersionStatus.DRAFT, VersionStatus.PENDING_APPROVAL);

    // ===================================================================
    // EM-8 — Author (unchanged in shape from V1; see V1 notes)
    // ===================================================================
    @Override
    public EmailTemplateDetailResponse createDraft(TemplateDetailsRequest request, String currentUser) {
        if (templateRepository.existsByTemplateNameIgnoreCase(request.getTemplateName())) {
            throw new IllegalArgumentException(EmailTemplateConstants.MSG_TEMPLATE_NAME_DUPLICATE);
        }
        if (!dataSourceQueryPort.isAuthorized(currentUser, request.getDataSourceQueryId())) {
            throw new SecurityException(EmailTemplateConstants.MSG_UNAUTHORIZED_QUERY);
        }
        int approvedQueryVersion = dataSourceQueryPort.getApprovedVersion(request.getDataSourceQueryId());

        EmailTemplate template = EmailTemplate.builder()
                .templateName(request.getTemplateName())
                .status(TemplateStatus.DRAFT)
                .ownerUserId(currentUser)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();
        template = templateRepository.save(template);

        EmailTemplateVersion version = EmailTemplateVersion.builder()
                .template(template)
                .versionNumber(1)
                .fromIdentityId(request.getFromIdentityId())
                .dataSourceQueryId(request.getDataSourceQueryId())
                .dataSourceQueryVersion(approvedQueryVersion)
                .subject("")
                .bodyHtml("")
                .bodyPlainText("")
                .bodySizeBytes(0)
                .recipientMode(RecipientMode.DEFINE_AT_SEND)
                .recipientEmailColumn("")
                .status(VersionStatus.DRAFT)
                .hasMergeFieldConflict(false)
                .hasRecipientMappingConflict(false)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();
        version = versionRepository.save(version);

        template.setCurrentVersion(version);
        templateRepository.save(template);

        audit(template.getTemplateId(), version.getTemplateVersionId(), "CREATED", currentUser, null);
        return toDetailResponse(template, version);
    }

    @Override
    public EmailTemplateDetailResponse updateContent(Long templateId, EmailContentRequest request, String currentUser) {
        EmailTemplate template = getTemplateOrThrow(templateId);
        EmailTemplateVersion version = getEditableCurrentVersion(template);

        version.setSubject(request.getSubject());
        String sanitizedHtml = richTextSanitizerPort.sanitize(request.getBodyHtml());
        version.setBodyHtml(sanitizedHtml);
        version.setBodyPlainText(richTextSanitizerPort.toPlainText(sanitizedHtml));
        version.setBodySizeBytes(sanitizedHtml.getBytes().length);

        if (version.getBodySizeBytes() > EmailTemplateConstants.BODY_MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Formatted body exceeds the maximum allowed size.");
        }

        mergeFieldRepository.deleteAll(mergeFieldRepository.findByTemplateVersion_TemplateVersionId(version.getTemplateVersionId()));
        List<String> validColumns = dataSourceQueryPort.getColumns(version.getDataSourceQueryId(), version.getDataSourceQueryVersion());
        boolean anyBroken = false;
        if (request.getMergeFields() != null) {
            for (MergeFieldRequest mf : request.getMergeFields()) {
                boolean broken = !validColumns.contains(mf.getSourceColumn());
                anyBroken = anyBroken || broken;
                mergeFieldRepository.save(EmailTemplateMergeField.builder()
                        .templateVersion(version)
                        .placeholderName(mf.getPlaceholderName())
                        .sourceColumn(mf.getSourceColumn())
                        .fieldLocation(mf.getFieldLocation())
                        .isBroken(broken)
                        .build());
            }
        }
        version.setHasMergeFieldConflict(anyBroken);
        version.setUpdatedBy(currentUser);
        versionRepository.save(version);

        audit(templateId, version.getTemplateVersionId(), "CONTENT_UPDATED", currentUser, null);
        return toDetailResponse(template, version);
    }

    @Override
    public EmailTemplateDetailResponse updateRecipients(Long templateId, RecipientsRequest request, String currentUser) {
        EmailTemplate template = getTemplateOrThrow(templateId);
        EmailTemplateVersion version = getEditableCurrentVersion(template);

        version.setRecipientMode(request.getRecipientMode());
        version.setRecipientEmailColumn(request.getRecipientEmailColumn());
        version.setRecipientNameColumn(request.getRecipientNameColumn());

        List<String> validColumns = dataSourceQueryPort.getColumns(version.getDataSourceQueryId(), version.getDataSourceQueryVersion());
        boolean mappingBroken = !isBlank(request.getRecipientEmailColumn()) && !validColumns.contains(request.getRecipientEmailColumn());
        mappingBroken = mappingBroken || (!isBlank(request.getRecipientNameColumn()) && !validColumns.contains(request.getRecipientNameColumn()));
        version.setHasRecipientMappingConflict(mappingBroken);

        version.setUpdatedBy(currentUser);
        versionRepository.save(version);

        recipientSelectionRepository.deleteByTemplateVersion_TemplateVersionId(version.getTemplateVersionId());
        if (request.getRecipientMode() == RecipientMode.CONTACT_DISTRIBUTION_LIST) {
            if (request.getDistributionListIds() != null) {
                request.getDistributionListIds().forEach(listId ->
                        recipientSelectionRepository.save(EmailTemplateRecipientSelection.builder()
                                .templateVersion(version)
                                .distributionListId(listId)
                                .build()));
            }
            if (request.getContactIds() != null) {
                request.getContactIds().forEach(contactId ->
                        recipientSelectionRepository.save(EmailTemplateRecipientSelection.builder()
                                .templateVersion(version)
                                .contactId(contactId)
                                .build()));
            }
        }

        audit(templateId, version.getTemplateVersionId(), "RECIPIENTS_UPDATED", currentUser, request.getRecipientMode().name());
        return toDetailResponse(template, version);
    }

    @Override
    public EmailTemplateDetailResponse addAttachment(Long templateId, MultipartFile file, String currentUser) {
        EmailTemplate template = getTemplateOrThrow(templateId);
        EmailTemplateVersion version = getEditableCurrentVersion(template);

        if (file.getSize() > EmailTemplateConstants.ATTACHMENT_MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(EmailTemplateConstants.MSG_ATTACHMENT_TOO_LARGE);
        }
        String extension = getExtension(file.getOriginalFilename());
        boolean allowed = List.of(EmailTemplateConstants.ALLOWED_ATTACHMENT_EXTENSIONS)
                .contains(extension.toLowerCase(Locale.ROOT));
        if (!allowed) {
            throw new IllegalArgumentException(EmailTemplateConstants.MSG_ATTACHMENT_TYPE_NOT_ALLOWED);
        }

        String storagePath = attachmentStoragePort.storeAndScan(file);

        attachmentRepository.save(EmailTemplateAttachment.builder()
                .templateVersion(version)
                .fileName(file.getOriginalFilename())
                .fileExtension(extension)
                .fileSizeBytes(file.getSize())
                .storagePath(storagePath)
                .virusScanStatus(VirusScanStatus.CLEAN)
                .uploadedBy(currentUser)
                .build());

        audit(templateId, version.getTemplateVersionId(), "ATTACHMENT_ADDED", currentUser, file.getOriginalFilename());
        return toDetailResponse(template, version);
    }

    @Override
    public EmailTemplateDetailResponse adoptLatestQueryVersion(Long templateId, String currentUser) {
        EmailTemplate template = getTemplateOrThrow(templateId);
        EmailTemplateVersion version = getEditableCurrentVersion(template);

        int latest = dataSourceQueryPort.getApprovedVersion(version.getDataSourceQueryId());
        version.setDataSourceQueryVersion(latest);
        version.setNewerQueryVersionAvailable(null);

        List<String> validColumns = dataSourceQueryPort.getColumns(version.getDataSourceQueryId(), latest);

        boolean anyBroken = false;
        for (EmailTemplateMergeField mf : mergeFieldRepository.findByTemplateVersion_TemplateVersionId(version.getTemplateVersionId())) {
            boolean broken = !validColumns.contains(mf.getSourceColumn());
            mf.setIsBroken(broken);
            mergeFieldRepository.save(mf);
            anyBroken = anyBroken || broken;
        }
        version.setHasMergeFieldConflict(anyBroken);

        boolean mappingBroken = !isBlank(version.getRecipientEmailColumn()) && !validColumns.contains(version.getRecipientEmailColumn());
        mappingBroken = mappingBroken || (!isBlank(version.getRecipientNameColumn()) && !validColumns.contains(version.getRecipientNameColumn()));
        version.setHasRecipientMappingConflict(mappingBroken);

        version.setUpdatedBy(currentUser);
        versionRepository.save(version);

        audit(templateId, version.getTemplateVersionId(), "QUERY_VERSION_ADOPTED", currentUser, "v" + latest);
        return toDetailResponse(template, version);
    }

    @Override
    public EmailTemplateDetailResponse submitForApproval(Long templateId, SubmitForApprovalRequest request, String currentUser) {
        EmailTemplate template = getTemplateOrThrow(templateId);
        EmailTemplateVersion version = getEditableCurrentVersion(template);

        ensureNoPendingChange(templateId);
        if (Boolean.TRUE.equals(version.getHasMergeFieldConflict())) {
            throw new IllegalStateException(EmailTemplateConstants.MSG_MERGE_FIELD_CONFLICT);
        }
        if (Boolean.TRUE.equals(version.getHasRecipientMappingConflict())) {
            throw new IllegalStateException(EmailTemplateConstants.MSG_MERGE_FIELD_CONFLICT);
        }

        version.setStatus(VersionStatus.PENDING_APPROVAL);
        version.setUpdatedBy(currentUser);
        versionRepository.save(version);

        ChangeType changeType = version.getVersionNumber() == 1 ? ChangeType.NEW_TEMPLATE : ChangeType.EDIT;
        EmailTemplateChangeRequest cr = EmailTemplateChangeRequest.builder()
                .template(template)
                .changeType(changeType)
                .templateVersion(version)
                .reason(request.getComments())
                .status(ChangeRequestStatus.PENDING)
                .submittedBy(currentUser)
                .build();
        changeRequestRepository.save(cr);

        audit(templateId, version.getTemplateVersionId(), "SUBMITTED", currentUser, request.getComments());
        return toDetailResponse(template, version);
    }

    // ===================================================================
    // EM-9 — Edit an existing active template
    // ===================================================================
    @Override
    public EmailTemplateDetailResponse startEdit(Long templateId, String currentUser) {
        EmailTemplate template = getTemplateOrThrow(templateId);
        if (template.getStatus() != TemplateStatus.ACTIVE || template.getActiveVersion() == null) {
            throw new IllegalStateException(EmailTemplateConstants.MSG_TEMPLATE_NOT_ACTIVE_FOR_EDIT);
        }
        if (!versionRepository.findByTemplate_TemplateIdAndStatusIn(templateId, OPEN_VERSION_STATUSES).isEmpty()) {
            throw new IllegalStateException(EmailTemplateConstants.MSG_PENDING_CHANGE_EXISTS);
        }

        EmailTemplateVersion active = template.getActiveVersion();
        int nextVersionNumber = versionRepository.findTopByTemplate_TemplateIdOrderByVersionNumberDesc(templateId)
                .map(EmailTemplateVersion::getVersionNumber).orElse(active.getVersionNumber()) + 1;

        EmailTemplateVersion draft = copyVersionContent(active, template, nextVersionNumber, currentUser, null);
        template.setCurrentVersion(draft);
        template.setUpdatedBy(currentUser);
        templateRepository.save(template);

        audit(templateId, draft.getTemplateVersionId(), "EDIT_STARTED", currentUser, "from v" + active.getVersionNumber());
        return toDetailResponse(template, draft);
    }

    // ===================================================================
    // EM-10 — Retire / Reactivate
    // ===================================================================
    @Override
    public ChangeRequestResponse retire(Long templateId, LifecycleRequest request, String currentUser) {
        EmailTemplate template = getTemplateOrThrow(templateId);
        if (template.getStatus() != TemplateStatus.ACTIVE) {
            throw new IllegalStateException(EmailTemplateConstants.MSG_TEMPLATE_NOT_ACTIVE_FOR_RETIRE);
        }
        ensureNoPendingChange(templateId);

        EmailTemplateChangeRequest cr = EmailTemplateChangeRequest.builder()
                .template(template)
                .changeType(ChangeType.RETIRE)
                .status(ChangeRequestStatus.PENDING)
                .reason(request.getReason())
                .submittedBy(currentUser)
                .build();
        cr = changeRequestRepository.save(cr);

        audit(templateId, null, "RETIRE_REQUESTED", currentUser, request.getReason());
        return toChangeRequestResponse(cr);
    }

    @Override
    public ChangeRequestResponse reactivate(Long templateId, LifecycleRequest request, String currentUser) {
        EmailTemplate template = getTemplateOrThrow(templateId);
        if (template.getStatus() != TemplateStatus.RETIRED) {
            throw new IllegalStateException(EmailTemplateConstants.MSG_TEMPLATE_NOT_RETIRED_FOR_REACTIVATE);
        }
        ensureNoPendingChange(templateId);

        EmailTemplateChangeRequest cr = EmailTemplateChangeRequest.builder()
                .template(template)
                .changeType(ChangeType.REACTIVATE)
                .status(ChangeRequestStatus.PENDING)
                .reason(request.getReason())
                .submittedBy(currentUser)
                .build();
        cr = changeRequestRepository.save(cr);

        audit(templateId, null, "REACTIVATE_REQUESTED", currentUser, request.getReason());
        return toChangeRequestResponse(cr);
    }

    // ===================================================================
    // EM-11 — Version history / compare
    // ===================================================================
    @Override
    @Transactional(readOnly = true)
    public List<VersionHistoryEntryResponse> getVersionHistory(Long templateId, Set<String> roles) {
        EmailTemplate template = getTemplateOrThrow(templateId);
        List<EmailTemplateVersion> versions;
        if (hasHistoryAccess(roles)) {
            versions = versionRepository.findByTemplate_TemplateIdOrderByVersionNumberDesc(templateId);
        } else {
            versions = template.getActiveVersion() != null ? List.of(template.getActiveVersion()) : List.of();
        }
        return versions.stream().map(this::toHistoryEntry).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VersionCompareResponse compareVersions(Long templateId, Long versionAId, Long versionBId, Set<String> roles) {
        if (!hasHistoryAccess(roles)) {
            throw new SecurityException(EmailTemplateConstants.MSG_COMPARE_REQUIRES_HISTORY_ACCESS);
        }
        EmailTemplate template = getTemplateOrThrow(templateId);
        EmailTemplateVersion a = getVersionOrThrow(templateId, versionAId);
        EmailTemplateVersion b = getVersionOrThrow(templateId, versionBId);

        List<String> changed = new ArrayList<>();
        if (!Objects.equals(a.getSubject(), b.getSubject())) changed.add("subject");
        if (!Objects.equals(a.getBodyHtml(), b.getBodyHtml())) changed.add("bodyHtml");
        if (a.getRecipientMode() != b.getRecipientMode()) changed.add("recipientMode");
        if (!Objects.equals(a.getRecipientEmailColumn(), b.getRecipientEmailColumn())) changed.add("recipientEmailColumn");
        if (!Objects.equals(a.getRecipientNameColumn(), b.getRecipientNameColumn())) changed.add("recipientNameColumn");
        if (!Objects.equals(a.getDataSourceQueryVersion(), b.getDataSourceQueryVersion())) changed.add("dataSourceQueryVersion");
        if (!attachmentNames(a).equals(attachmentNames(b))) changed.add("attachments");

        return VersionCompareResponse.builder()
                .versionA(toDetailResponse(template, a))
                .versionB(toDetailResponse(template, b))
                .changedFields(changed)
                .build();
    }

    // ===================================================================
    // EM-12 — Restore
    // ===================================================================
    @Override
    public ChangeRequestResponse restore(Long templateId, RestoreRequest request, String currentUser) {
        EmailTemplate template = getTemplateOrThrow(templateId);
        EmailTemplateVersion source = getVersionOrThrow(templateId, request.getSourceVersionId());
        if (source.getStatus() != VersionStatus.ACTIVE && source.getStatus() != VersionStatus.SUPERSEDED) {
            throw new IllegalArgumentException(EmailTemplateConstants.MSG_RESTORE_SOURCE_INVALID);
        }
        ensureNoPendingChange(templateId);

        int nextVersionNumber = versionRepository.findTopByTemplate_TemplateIdOrderByVersionNumberDesc(templateId)
                .map(EmailTemplateVersion::getVersionNumber).orElse(source.getVersionNumber()) + 1;

        EmailTemplateVersion draft = copyVersionContent(source, template, nextVersionNumber, currentUser, source);
        draft.setStatus(VersionStatus.PENDING_APPROVAL);
        versionRepository.save(draft);

        template.setCurrentVersion(draft);
        template.setUpdatedBy(currentUser);
        templateRepository.save(template);

        EmailTemplateChangeRequest cr = EmailTemplateChangeRequest.builder()
                .template(template)
                .changeType(ChangeType.RESTORE)
                .templateVersion(draft)
                .restoreFromVersion(source)
                .status(ChangeRequestStatus.PENDING)
                .submittedBy(currentUser)
                .build();
        cr = changeRequestRepository.save(cr);

        audit(templateId, draft.getTemplateVersionId(), "RESTORE_SUBMITTED", currentUser, "from v" + source.getVersionNumber());
        return toChangeRequestResponse(cr);
    }

    // ===================================================================
    // EM-13 — Approval queue (all change types)
    // ===================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<ChangeRequestResponse> getApprovalQueue(ChangeType changeTypeFilter, Pageable pageable) {
        Page<EmailTemplateChangeRequest> page = changeTypeFilter == null
                ? changeRequestRepository.findByStatus(ChangeRequestStatus.PENDING, pageable)
                : changeRequestRepository.findByStatusAndChangeType(ChangeRequestStatus.PENDING, changeTypeFilter, pageable);
        return page.map(this::toChangeRequestResponse);
    }

    @Override
    public ChangeRequestResponse approve(Long changeRequestId, ApprovalDecisionRequest request, String currentUser) {
        EmailTemplateChangeRequest cr = getChangeRequestOrThrow(changeRequestId);
        requirePending(cr);
        if (cr.getSubmittedBy().equals(currentUser)) {
            throw new SecurityException(EmailTemplateConstants.MSG_SELF_APPROVAL_BLOCKED);
        }

        EmailTemplate template = cr.getTemplate();
        switch (cr.getChangeType()) {
            case NEW_TEMPLATE, EDIT, RESTORE -> {
                EmailTemplateVersion version = cr.getTemplateVersion();
                EmailTemplateVersion previousActive = template.getActiveVersion();
                if (previousActive != null && !previousActive.getTemplateVersionId().equals(version.getTemplateVersionId())) {
                    previousActive.setStatus(VersionStatus.SUPERSEDED);
                    versionRepository.save(previousActive);
                }
                version.setStatus(VersionStatus.ACTIVE);
                versionRepository.save(version);
                template.setActiveVersion(version);
                template.setStatus(TemplateStatus.ACTIVE);
            }
            case RETIRE -> template.setStatus(TemplateStatus.RETIRED);
            case REACTIVATE -> template.setStatus(TemplateStatus.ACTIVE);
        }
        template.setUpdatedBy(currentUser);
        templateRepository.save(template);

        cr.setStatus(ChangeRequestStatus.APPROVED);
        cr.setDecidedBy(currentUser);
        cr.setDecidedAt(java.time.OffsetDateTime.now());
        cr.setDecisionComments(request.getComments());
        cr = changeRequestRepository.save(cr);

        audit(template.getTemplateId(), cr.getTemplateVersion() != null ? cr.getTemplateVersion().getTemplateVersionId() : null,
                "APPROVED", currentUser, request.getComments());
        return toChangeRequestResponse(cr);
    }

    @Override
    public ChangeRequestResponse reject(Long changeRequestId, ApprovalDecisionRequest request, String currentUser) {
        EmailTemplateChangeRequest cr = getChangeRequestOrThrow(changeRequestId);
        requirePending(cr);
        if (cr.getSubmittedBy().equals(currentUser)) {
            throw new SecurityException(EmailTemplateConstants.MSG_SELF_APPROVAL_BLOCKED);
        }

        if (cr.getChangeType() == ChangeType.NEW_TEMPLATE || cr.getChangeType() == ChangeType.EDIT || cr.getChangeType() == ChangeType.RESTORE) {
            EmailTemplateVersion version = cr.getTemplateVersion();
            version.setStatus(VersionStatus.REJECTED);
            versionRepository.save(version);
        }
        // RETIRE / REACTIVATE: rejecting simply leaves the template's current status untouched.

        cr.setStatus(ChangeRequestStatus.REJECTED);
        cr.setRejectionReason(request.getRejectionReason());
        cr.setDecidedBy(currentUser);
        cr.setDecidedAt(java.time.OffsetDateTime.now());
        cr.setDecisionComments(request.getComments());
        cr = changeRequestRepository.save(cr);

        audit(cr.getTemplate().getTemplateId(), cr.getTemplateVersion() != null ? cr.getTemplateVersion().getTemplateVersionId() : null,
                "REJECTED", currentUser, request.getRejectionReason());
        return toChangeRequestResponse(cr);
    }

    @Override
    public ChangeRequestResponse withdraw(Long changeRequestId, String currentUser, Set<String> roles) {
        EmailTemplateChangeRequest cr = getChangeRequestOrThrow(changeRequestId);
        requirePending(cr);

        boolean isSubmitter = cr.getSubmittedBy().equals(currentUser);
        boolean isOnBehalfRole = roles != null && Arrays.stream(SecurityRoles.WITHDRAW_ON_BEHALF_ROLES).anyMatch(roles::contains);
        if (!isSubmitter && !isOnBehalfRole) {
            throw new SecurityException(EmailTemplateConstants.MSG_WITHDRAW_NOT_ALLOWED);
        }

        if (cr.getChangeType() == ChangeType.NEW_TEMPLATE || cr.getChangeType() == ChangeType.EDIT || cr.getChangeType() == ChangeType.RESTORE) {
            EmailTemplateVersion version = cr.getTemplateVersion();
            version.setStatus(VersionStatus.WITHDRAWN);
            versionRepository.save(version);
        }

        cr.setStatus(ChangeRequestStatus.WITHDRAWN);
        cr.setDecidedBy(currentUser);
        cr.setDecidedAt(java.time.OffsetDateTime.now());
        cr = changeRequestRepository.save(cr);

        audit(cr.getTemplate().getTemplateId(), cr.getTemplateVersion() != null ? cr.getTemplateVersion().getTemplateVersionId() : null,
                "WITHDRAWN", currentUser, null);
        return toChangeRequestResponse(cr);
    }

    // ===================================================================
    // Ownership / lookup
    // ===================================================================
    @Override
    public EmailTemplateDetailResponse reassignOwner(Long templateId, String newOwnerUserId, String currentUser) {
        EmailTemplate template = getTemplateOrThrow(templateId);
        template.setOwnerUserId(newOwnerUserId);
        template.setUpdatedBy(currentUser);
        templateRepository.save(template);

        audit(templateId, null, "OWNER_REASSIGNED", currentUser, "new owner: " + newOwnerUserId);
        return toDetailResponse(template, template.getCurrentVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public EmailTemplateDetailResponse getById(Long templateId) {
        EmailTemplate template = getTemplateOrThrow(templateId);
        return toDetailResponse(template, template.getCurrentVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailTemplateSummaryResponse> list(Pageable pageable, Set<String> roles) {
        Page<EmailTemplate> page = hasHistoryAccess(roles)
                ? templateRepository.findAll(pageable)
                : templateRepository.findByStatus(TemplateStatus.ACTIVE, pageable);

        return page.map(t -> EmailTemplateSummaryResponse.builder()
                .templateId(t.getTemplateId())
                .templateName(t.getTemplateName())
                .subject(t.getCurrentVersion() != null ? t.getCurrentVersion().getSubject() : null)
                .version(t.getCurrentVersion() != null ? t.getCurrentVersion().getVersionNumber() : null)
                .status(t.getStatus())
                .ownerUserId(t.getOwnerUserId())
                .updatedAt(t.getUpdatedAt())
                .build());
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------
    private EmailTemplate getTemplateOrThrow(Long templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException(EmailTemplateConstants.MSG_TEMPLATE_NOT_FOUND));
    }

    private EmailTemplateVersion getVersionOrThrow(Long templateId, Long versionId) {
        EmailTemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new EntityNotFoundException(EmailTemplateConstants.MSG_VERSION_NOT_FOUND));
        if (!version.getTemplate().getTemplateId().equals(templateId)) {
            throw new EntityNotFoundException(EmailTemplateConstants.MSG_VERSION_NOT_FOUND);
        }
        return version;
    }

    private EmailTemplateChangeRequest getChangeRequestOrThrow(Long changeRequestId) {
        return changeRequestRepository.findById(changeRequestId)
                .orElseThrow(() -> new EntityNotFoundException(EmailTemplateConstants.MSG_CHANGE_REQUEST_NOT_FOUND));
    }

    private EmailTemplateVersion getEditableCurrentVersion(EmailTemplate template) {
        EmailTemplateVersion version = template.getCurrentVersion();
        if (version == null) {
            throw new EntityNotFoundException(EmailTemplateConstants.MSG_VERSION_NOT_FOUND);
        }
        if (version.getStatus() != VersionStatus.DRAFT) {
            throw new IllegalStateException(EmailTemplateConstants.MSG_NOT_EDITABLE);
        }
        return version;
    }

    private void requirePending(EmailTemplateChangeRequest cr) {
        if (cr.getStatus() != ChangeRequestStatus.PENDING) {
            throw new IllegalStateException(EmailTemplateConstants.MSG_CHANGE_REQUEST_NOT_PENDING);
        }
    }

    private void ensureNoPendingChange(Long templateId) {
        if (changeRequestRepository.findByTemplate_TemplateIdAndStatus(templateId, ChangeRequestStatus.PENDING).isPresent()) {
            throw new IllegalStateException(EmailTemplateConstants.MSG_PENDING_CHANGE_EXISTS);
        }
    }

    private boolean hasHistoryAccess(Set<String> roles) {
        if (roles == null) return false;
        return Arrays.stream(SecurityRoles.TEMPLATE_HISTORY_ROLES).anyMatch(roles::contains);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private Set<String> attachmentNames(EmailTemplateVersion version) {
        return version.getAttachments().stream().map(EmailTemplateAttachment::getFileName).collect(Collectors.toSet());
    }

    /** Deep-copies content (subject/body/merge fields/recipients/attachments) from source into a brand new version row. */
    private EmailTemplateVersion copyVersionContent(EmailTemplateVersion source, EmailTemplate template, int newVersionNumber,
                                                      String currentUser, EmailTemplateVersion restoredFrom) {
        EmailTemplateVersion copy = EmailTemplateVersion.builder()
                .template(template)
                .versionNumber(newVersionNumber)
                .fromIdentityId(source.getFromIdentityId())
                .dataSourceQueryId(source.getDataSourceQueryId())
                .dataSourceQueryVersion(source.getDataSourceQueryVersion())
                .subject(source.getSubject())
                .bodyHtml(source.getBodyHtml())
                .bodyPlainText(source.getBodyPlainText())
                .bodySizeBytes(source.getBodySizeBytes())
                .recipientMode(source.getRecipientMode())
                .recipientEmailColumn(source.getRecipientEmailColumn())
                .recipientNameColumn(source.getRecipientNameColumn())
                .status(VersionStatus.DRAFT)
                .hasMergeFieldConflict(false)
                .hasRecipientMappingConflict(false)
                .restoredFromVersion(restoredFrom)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();
        copy = versionRepository.save(copy);

        for (EmailTemplateMergeField mf : source.getMergeFields()) {
            mergeFieldRepository.save(EmailTemplateMergeField.builder()
                    .templateVersion(copy)
                    .placeholderName(mf.getPlaceholderName())
                    .sourceColumn(mf.getSourceColumn())
                    .fieldLocation(mf.getFieldLocation())
                    .isBroken(false)
                    .build());
        }
        for (EmailTemplateAttachment att : source.getAttachments()) {
            attachmentRepository.save(EmailTemplateAttachment.builder()
                    .templateVersion(copy)
                    .fileName(att.getFileName())
                    .fileExtension(att.getFileExtension())
                    .fileSizeBytes(att.getFileSizeBytes())
                    .storagePath(att.getStoragePath())
                    .virusScanStatus(att.getVirusScanStatus())
                    .uploadedBy(currentUser)
                    .build());
        }
        for (EmailTemplateRecipientSelection sel : source.getRecipientSelections()) {
            recipientSelectionRepository.save(EmailTemplateRecipientSelection.builder()
                    .templateVersion(copy)
                    .distributionListId(sel.getDistributionListId())
                    .contactId(sel.getContactId())
                    .build());
        }
        return copy;
    }

    private void audit(Long templateId, Long versionId, String action, String user, String detail) {
        auditLogRepository.save(EmailTemplateAuditLog.builder()
                .templateId(templateId)
                .templateVersionId(versionId)
                .action(action)
                .performedBy(user)
                .detail(detail)
                .build());
    }

    private EmailTemplateDetailResponse toDetailResponse(EmailTemplate template, EmailTemplateVersion version) {
        EmailTemplateDetailResponse.EmailTemplateDetailResponseBuilder builder = EmailTemplateDetailResponse.builder()
                .templateId(template.getTemplateId())
                .templateName(template.getTemplateName())
                .templateStatus(template.getStatus());

        if (version != null) {
            builder.templateVersionId(version.getTemplateVersionId())
                    .versionNumber(version.getVersionNumber())
                    .versionStatus(version.getStatus())
                    .dataSourceQueryId(version.getDataSourceQueryId())
                    .dataSourceQueryVersion(version.getDataSourceQueryVersion())
                    .hasMergeFieldConflict(version.getHasMergeFieldConflict())
                    .newerQueryVersionAvailable(version.getNewerQueryVersionAvailable())
                    .subject(version.getSubject())
                    .bodyHtml(version.getBodyHtml())
                    .recipientMode(version.getRecipientMode())
                    .recipientEmailColumn(version.getRecipientEmailColumn())
                    .recipientNameColumn(version.getRecipientNameColumn())
                    .attachmentFileNames(version.getAttachments().stream()
                            .map(EmailTemplateAttachment::getFileName).collect(Collectors.toList()));
        }
        return builder.build();
    }

    private ChangeRequestResponse toChangeRequestResponse(EmailTemplateChangeRequest cr) {
        return ChangeRequestResponse.builder()
                .changeRequestId(cr.getChangeRequestId())
                .templateId(cr.getTemplate().getTemplateId())
                .templateName(cr.getTemplate().getTemplateName())
                .changeType(cr.getChangeType())
                .status(cr.getStatus())
                .templateVersionId(cr.getTemplateVersion() != null ? cr.getTemplateVersion().getTemplateVersionId() : null)
                .versionNumber(cr.getTemplateVersion() != null ? cr.getTemplateVersion().getVersionNumber() : null)
                .reason(cr.getReason())
                .rejectionReason(cr.getRejectionReason())
                .submittedBy(cr.getSubmittedBy())
                .submittedAt(cr.getSubmittedAt())
                .decidedBy(cr.getDecidedBy())
                .decidedAt(cr.getDecidedAt())
                .decisionComments(cr.getDecisionComments())
                .build();
    }

    private VersionHistoryEntryResponse toHistoryEntry(EmailTemplateVersion version) {
        Optional<EmailTemplateChangeRequest> cr = changeRequestRepository
                .findByTemplate_TemplateIdOrderBySubmittedAtDesc(version.getTemplate().getTemplateId()).stream()
                .filter(c -> c.getTemplateVersion() != null
                        && c.getTemplateVersion().getTemplateVersionId().equals(version.getTemplateVersionId()))
                .findFirst();

        VersionHistoryEntryResponse.VersionHistoryEntryResponseBuilder builder = VersionHistoryEntryResponse.builder()
                .templateVersionId(version.getTemplateVersionId())
                .versionNumber(version.getVersionNumber())
                .status(version.getStatus())
                .subject(version.getSubject())
                .restoredFromVersionNumber(version.getRestoredFromVersion() != null
                        ? Long.valueOf(version.getRestoredFromVersion().getVersionNumber()) : null);

        cr.ifPresent(c -> builder.submittedBy(c.getSubmittedBy())
                .submittedAt(c.getSubmittedAt())
                .decidedBy(c.getDecidedBy())
                .decidedAt(c.getDecidedAt()));

        return builder.build();
    }
}
