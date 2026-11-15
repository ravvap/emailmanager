package gov.fdic.tip.emailmanager.sendemail.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fdic.tip.emailmanager.common.constants.EmailTemplateConstants;
import com.fdic.tip.emailmanager.template.dto.EmailTemplateSummaryResponse;
import com.fdic.tip.emailmanager.template.entity.*;
import com.fdic.tip.emailmanager.template.enums.RecipientMode;
import com.fdic.tip.emailmanager.template.enums.TemplateStatus;
import com.fdic.tip.emailmanager.template.repository.EmailTemplateFileRecipientRepository;
import com.fdic.tip.emailmanager.template.repository.EmailTemplateRepository;
import com.fdic.tip.emailmanager.template.service.ContactDirectoryPort;
import com.fdic.tip.emailmanager.template.service.DataSourceQueryPort;
import com.fdic.tip.emailmanager.template.service.DistributionListDirectoryPort;
import com.fdic.tip.emailmanager.template.service.RecipientFileParserPort;
import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants;
import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.*;
import gov.fdic.tip.emailmanager.sendemail.dto.*;
import gov.fdic.tip.emailmanager.sendemail.entity.*;
import gov.fdic.tip.emailmanager.sendemail.port.EmailDispatchEnginePort;
import gov.fdic.tip.emailmanager.sendemail.port.InternalDomainAllowlistPort;
import gov.fdic.tip.emailmanager.sendemail.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailSendService {

    private final EmailSendRepository sendRepository;
    private final EmailSendRecipientRepository sendRecipientRepository;
    private final EmailSendAttachmentRepository sendAttachmentRepository;
    private final EmailSendAuditLogRepository sendAuditLogRepository;

    private final EmailTemplateRepository templateRepository;
    private final EmailTemplateFileRecipientRepository templateFileRecipientRepository;

    private final DataSourceQueryPort dataSourceQueryPort;
    private final ContactDirectoryPort contactDirectoryPort;
    private final DistributionListDirectoryPort distributionListDirectoryPort;
    private final RecipientFileParserPort recipientFileParserPort;
    private final InternalDomainAllowlistPort allowlistPort;
    private final EmailDispatchEnginePort dispatchEnginePort;
    private final ObjectMapper objectMapper;

    private final Map<String, List<ResolvedRecipient>> uploadStagingStore = new ConcurrentHashMap<>();

    // ===================================================================
    // 1. Active Template Selection
    // ===================================================================
    
    @Transactional(readOnly = true)
    public List<EmailTemplateSummaryResponse> getActiveApprovedTemplates() {
        return templateRepository.findByStatusAndDeletedAtIsNull(TemplateStatus.ACTIVE, Pageable.unpaged())
                .stream()
                .filter(t -> t.getActiveVersion() != null)
                .map(t -> EmailTemplateSummaryResponse.builder()
                        .templateId(t.getTemplateId())
                        .templateName(t.getTemplateName())
                        .subject(t.getActiveVersion().getSubject())
                        .version(t.getActiveVersion().getVersionNumber())
                        .status(t.getStatus())
                        .ownerUserId(t.getOwnerUserId())
                        .updatedAt(t.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ===================================================================
    // 2. Send via Excel Recipient Upload (Ad-hoc upload at send time)
    // ===================================================================
    
    public SendFileUploadResponse uploadAdHocRecipientFile(Long templateId, MultipartFile file) {
        EmailTemplate template = getActiveTemplateOrThrow(templateId);
        EmailTemplateVersion version = template.getActiveVersion();

        if (version.getRecipientMode() != RecipientMode.FILE_UPLOAD
                && version.getRecipientMode() != RecipientMode.DEFINE_AT_SEND) {
            throw new IllegalStateException(AppConstants.MSG_FILE_UPLOAD_NOT_ALLOWED);
        }

        byte[] content = readBytes(file);
        List<String> sheets = recipientFileParserPort.listSheetNames(content, file.getOriginalFilename());
        String targetSheet = sheets.isEmpty() ? AppConstants.DEFAULT_EXCEL_SHEET_NAME : sheets.get(0);

        String emailCol = version.getRecipientEmailColumn() != null
                ? version.getRecipientEmailColumn() : AppConstants.DEFAULT_EMAIL_COLUMN;
        String nameCol = version.getRecipientNameColumn() != null
                ? version.getRecipientNameColumn() : AppConstants.DEFAULT_NAME_COLUMN;

        var rawRows = recipientFileParserPort.getRecipientRows(
                content, file.getOriginalFilename(), targetSheet, emailCol, nameCol);

        List<ResolvedRecipient> validRows = new ArrayList<>();
        List<String> skippedErrors = new ArrayList<>();

        for (var row : rawRows) {
            if (row.recipientEmail() == null || !row.recipientEmail().contains("@")) {
                skippedErrors.add("Row " + row.rowNumber() + ": Invalid or missing email address");
            } else {
                validRows.add(new ResolvedRecipient(
                        null,
                        row.recipientName(),
                        row.recipientEmail().trim(),
                        Map.of(nameCol, row.recipientName() != null ? row.recipientName() : "")
                ));
            }
        }

        String token = UUID.randomUUID().toString();
        uploadStagingStore.put(token, validRows);

        return SendFileUploadResponse.builder()
                .uploadSessionToken(token)
                .fileName(file.getOriginalFilename())
                .validRowCount(validRows.size())
                .skippedRowCount(skippedErrors.size())
                .skippedRowErrors(skippedErrors)
                .build();
    }

    // ===================================================================
    // 3. Preview Send (1st Row Sample Data + Conflict & Domain Check)
    // ===================================================================
    
    @Transactional(readOnly = true)
    public SendPreviewResponse previewSend(SendPreviewRequest request) {
        EmailTemplate template = getActiveTemplateOrThrow(request.getTemplateId());
        EmailTemplateVersion version = template.getActiveVersion();

        ResolutionOutcome resolution = resolveRecipientsForMode(
                version, request.getQueryParameters(), request.getUploadSessionToken(), request.getAdHocRecipients());

        String renderedSampleHtml = renderHtmlWithFirstRow(version, resolution.recipients());

        int externalCount = (int) resolution.recipients().stream()
                .filter(r -> allowlistPort.classifyEmail(r.email()) == RecipientClassification.EXTERNAL)
                .count();
        int totalCount = resolution.recipients().size();

        List<EmailSendRecipientResponse> recipientDtos = resolution.recipients().stream()
                .map(r -> EmailSendRecipientResponse.builder()
                        .recipientName(r.name())
                        .recipientEmail(r.email())
                        .classification(allowlistPort.classifyEmail(r.email()))
                        .deliveryStatus(RecipientDeliveryStatus.PENDING)
                        .build())
                .collect(Collectors.toList());

        return SendPreviewResponse.builder()
                .templateId(template.getTemplateId())
                .templateVersionId(version.getTemplateVersionId())
                .templateName(template.getTemplateName())
                .fromIdentityId(version.getFromIdentityId())
                .recipientMode(version.getRecipientMode())
                .subject(version.getSubject())
                .renderedSampleBodyHtml(renderedSampleHtml)
                .attachmentFileNames(version.getAttachments().stream()
                        .map(EmailTemplateAttachment::getFileName).collect(Collectors.toList()))
                .totalRecipients(totalCount)
                .internalRecipients(totalCount - externalCount)
                .externalRecipients(externalCount)
                .requiresExternalApproval(externalCount > 0)
                .hasAddressConflict(!resolution.conflictingEmails().isEmpty())
                .conflictingAddresses(resolution.conflictingEmails())
                .recipients(recipientDtos)
                .skippedInvalidRows(resolution.skippedRows())
                .build();
    }

    // ===================================================================
    // 4. Submit Send (Freeze Content + Route Internal vs. External)
    // ===================================================================
    
    public EmailSendDetailResponse submitSend(SubmitEmailSendRequest request) {
        String currentUser = CurrentUserInfo.getCurrentUserEmail();
        EmailTemplate template = getActiveTemplateOrThrow(request.getTemplateId());
        EmailTemplateVersion version = template.getActiveVersion();

        ResolutionOutcome resolution = resolveRecipientsForMode(
                version, request.getQueryParameters(), request.getUploadSessionToken(), request.getAdHocRecipients());

        if (!resolution.conflictingEmails().isEmpty()) {
            throw new IllegalStateException(AppConstants.MSG_RECIPIENT_CONFLICT);
        }
        if (resolution.recipients().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.MSG_NO_VALID_RECIPIENTS);
        }

        int externalCount = (int) resolution.recipients().stream()
                .filter(r -> allowlistPort.classifyEmail(r.email()) == RecipientClassification.EXTERNAL)
                .count();

        EmailSendStatus initialStatus = externalCount > 0
                ? EmailSendStatus.PENDING_REVIEW
                : EmailSendStatus.QUEUED;

        EmailSend send = EmailSend.builder()
                .templateId(template.getTemplateId())
                .templateVersionId(version.getTemplateVersionId())
                .templateName(template.getTemplateName())
                .fromIdentityId(version.getFromIdentityId())
                .recipientMode(version.getRecipientMode())
                .frozenSubject(version.getSubject())
                .frozenBodyHtml(version.getBodyHtml())
                .frozenBodyPlainText(version.getBodyPlainText())
                .dataSourceQueryId(version.getDataSourceQueryId())
                .dataSourceQueryVersion(version.getDataSourceQueryVersion())
                .queryParametersJson(toJson(request.getQueryParameters()))
                .status(initialStatus)
                .totalRecipients(resolution.recipients().size())
                .externalRecipients(externalCount)
                .deliveredCount(0)
                .failedCount(0)
                .submittedBy(currentUser)
                .build();
        send = sendRepository.save(send);

        for (EmailTemplateAttachment att : version.getAttachments()) {
            sendAttachmentRepository.save(EmailSendAttachment.builder()
                    .emailSend(send)
                    .fileName(att.getFileName())
                    .fileExtension(att.getFileExtension())
                    .fileSizeBytes(att.getFileSizeBytes())
                    .fileContent(Arrays.copyOf(att.getFileContent(), att.getFileContent().length))
                    .build());
        }

        for (ResolvedRecipient r : resolution.recipients()) {
            sendRecipientRepository.save(EmailSendRecipient.builder()
                    .emailSend(send)
                    .contactId(r.contactId())
                    .recipientName(r.name())
                    .recipientEmail(r.email())
                    .classification(allowlistPort.classifyEmail(r.email()))
                    .mergeValuesJson(toJson(r.mergeValues()))
                    .deliveryStatus(RecipientDeliveryStatus.PENDING)
                    .bounceCategory(BounceCategory.NONE)
                    .retryCount(0)
                    .flaggedForContactReview(false)
                    .build());
        }

        if (request.getUploadSessionToken() != null) {
            uploadStagingStore.remove(request.getUploadSessionToken());
        }

        audit(send.getSendId(), AppConstants.AUDIT_SUBMITTED, currentUser,
                "Status=" + initialStatus + ", Total=" + send.getTotalRecipients() + ", External=" + externalCount);

        if (initialStatus == EmailSendStatus.QUEUED) {
            dispatchEnginePort.enqueueSend(send.getSendId());
        }

        return getSendDetail(send.getSendId());
    }

    // ===================================================================
    // 5. Cancel Send (Before Reviewer Decision or Before Dispatch Worker)
    // ===================================================================
    
    public EmailSendDetailResponse cancelSend(Long sendId) {
        String currentUser = CurrentUserInfo.getCurrentUserEmail();
        EmailSend send = getSendOrThrow(sendId);

        if (!send.getSubmittedBy().equals(currentUser)) {
            throw new SecurityException(AppConstants.MSG_CANCEL_UNAUTHORIZED);
        }
        if (send.getStatus() != EmailSendStatus.PENDING_REVIEW && send.getStatus() != EmailSendStatus.QUEUED) {
            throw new IllegalStateException(AppConstants.MSG_CANCEL_NOT_ALLOWED);
        }

        send.setStatus(EmailSendStatus.CANCELED);
        send.setCanceledAt(OffsetDateTime.now());
        sendRepository.save(send);

        audit(sendId, AppConstants.AUDIT_CANCELED, currentUser, "Canceled by submitter prior to dispatch");
        return getSendDetail(sendId);
    }

    // ===================================================================
    // 6. Send History & Detail Drill-Down + Failed CSV Export
    // ===================================================================
    
    @Transactional(readOnly = true)
    public Page<EmailSendSummaryResponse> getMySends(Pageable pageable) {
        String currentUser = CurrentUserInfo.getCurrentUserEmail();
        return sendRepository.findBySubmittedByOrderBySubmittedAtDesc(currentUser, pageable)
                .map(this::toSummaryResponse);
    }

    
    @Transactional(readOnly = true)
    public EmailSendDetailResponse getSendDetail(Long sendId) {
        String currentUser = CurrentUserInfo.getCurrentUserEmail();
        EmailSend send = getSendOrThrow(sendId);
        List<EmailSendRecipient> recipients = sendRecipientRepository.findByEmailSend_SendIdOrderBySendRecipientIdAsc(sendId);

        String outcomeMessage = switch (send.getStatus()) {
            case PENDING_REVIEW, REJECTED_BY_REVIEWER, CANCELED -> AppConstants.OUTCOME_NOT_DISPATCHED;
            case QUEUED, IN_PROGRESS -> AppConstants.OUTCOME_IN_PROGRESS;
            case COMPLETED -> String.format(AppConstants.OUTCOME_COMPLETED_FORMAT, send.getDeliveredCount(), send.getFailedCount());
        };

        boolean canCancel = send.getSubmittedBy().equals(currentUser)
                && (send.getStatus() == EmailSendStatus.PENDING_REVIEW || send.getStatus() == EmailSendStatus.QUEUED);
        boolean canExport = send.getStatus() == EmailSendStatus.COMPLETED && send.getFailedCount() > 0;

        String sampleHtml = send.getFrozenBodyHtml();
        if (!recipients.isEmpty()) {
            Map<String, String> firstRowMerge = fromJsonMap(recipients.get(0).getMergeValuesJson());
            sampleHtml = applyMergeFields(sampleHtml, firstRowMerge);
        }

        return EmailSendDetailResponse.builder()
                .sendId(send.getSendId())
                .templateId(send.getTemplateId())
                .templateVersionId(send.getTemplateVersionId())
                .templateName(send.getTemplateName())
                .recipientMode(send.getRecipientMode())
                .status(send.getStatus())
                .deliveryOutcomeMessage(outcomeMessage)
                .canCancel(canCancel)
                .canExportFailedCsv(canExport)
                .totalRecipients(send.getTotalRecipients())
                .externalRecipients(send.getExternalRecipients())
                .deliveredCount(send.getDeliveredCount())
                .failedCount(send.getFailedCount())
                .subject(send.getFrozenSubject())
                .renderedSampleBodyHtml(sampleHtml)
                .attachmentFileNames(send.getAttachments().stream()
                        .map(EmailSendAttachment::getFileName).collect(Collectors.toList()))
                .submittedBy(send.getSubmittedBy())
                .submittedAt(send.getSubmittedAt())
                .reviewedBy(send.getReviewedBy())
                .reviewedAt(send.getReviewedAt())
                .reviewComments(send.getReviewComments())
                .canceledAt(send.getCanceledAt())
                .completedAt(send.getCompletedAt())
                .recipients(recipients.stream().map(this::toRecipientResponse).collect(Collectors.toList()))
                .build();
    }

    
    @Transactional(readOnly = true)
    public byte[] exportFailedDeliveriesCsv(Long sendId) {
        EmailSend send = getSendOrThrow(sendId);
        if (send.getStatus() != EmailSendStatus.COMPLETED) {
            throw new IllegalStateException(AppConstants.MSG_CSV_EXPORT_ONLY_COMPLETED);
        }

        List<EmailSendRecipient> failed = sendRecipientRepository
                .findByEmailSend_SendIdAndDeliveryStatus(sendId, RecipientDeliveryStatus.FAILED_PERMANENT);

        StringBuilder csv = new StringBuilder(AppConstants.FAILED_DELIVERIES_CSV_HEADER);
        for (EmailSendRecipient r : failed) {
            csv.append(escapeCsv(r.getRecipientName())).append(",")
               .append(escapeCsv(r.getRecipientEmail())).append(",")
               .append(escapeCsv(r.getFailureReason())).append("\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ===================================================================
    // 7. External-Recipient Approval Queue (EM-15)
    // ===================================================================
    
    @Transactional(readOnly = true)
    public Page<EmailSendSummaryResponse> getExternalApprovalQueue(Pageable pageable) {
        return sendRepository
                .findByStatusAndExternalRecipientsGreaterThanOrderBySubmittedAtDesc(EmailSendStatus.PENDING_REVIEW, 0, pageable)
                .map(this::toSummaryResponse);
    }

    
    public EmailSendDetailResponse decideExternalApproval(Long sendId, ExternalApprovalDecisionRequest request) {
        String currentUser = CurrentUserInfo.getCurrentUserEmail();
        EmailSend send = getSendOrThrow(sendId);

        if (send.getStatus() != EmailSendStatus.PENDING_REVIEW) {
            throw new IllegalStateException(AppConstants.MSG_NOT_PENDING_REVIEW);
        }
        if (send.getSubmittedBy().equals(currentUser)) {
            throw new SecurityException(AppConstants.MSG_SELF_APPROVAL_BLOCKED);
        }

        send.setReviewedBy(currentUser);
        send.setReviewedAt(OffsetDateTime.now());
        send.setReviewComments(request.getComments());

        if (Boolean.TRUE.equals(request.getApproved())) {
            send.setStatus(EmailSendStatus.QUEUED);
            sendRepository.save(send);
            audit(sendId, AppConstants.AUDIT_APPROVED, currentUser, request.getComments());
            dispatchEnginePort.enqueueSend(sendId);
        } else {
            if (request.getComments() == null || request.getComments().isBlank()) {
                throw new IllegalArgumentException(AppConstants.MSG_REJECTION_REASON_REQUIRED);
            }
            send.setStatus(EmailSendStatus.REJECTED_BY_REVIEWER);
            sendRepository.save(send);
            audit(sendId, AppConstants.AUDIT_REJECTED, currentUser, request.getComments());
        }

        return getSendDetail(sendId);
    }

    // ===================================================================
    // 8. Dispatch Worker Hook (Query Re-Run + New External Allowlist Check)
    // ===================================================================
    
    public void prepareAndDispatch(Long sendId) {
        EmailSend send = getSendOrThrow(sendId);
        if (send.getStatus() != EmailSendStatus.QUEUED) {
            return;
        }

        if (send.getRecipientMode() == RecipientMode.DATA_SOURCE_QUERY) {
            Set<String> previouslyApprovedExternals = sendRecipientRepository
                    .findByEmailSend_SendIdOrderBySendRecipientIdAsc(sendId).stream()
                    .filter(r -> r.getClassification() == RecipientClassification.EXTERNAL)
                    .map(r -> r.getRecipientEmail().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            EmailTemplate template = getActiveTemplateOrThrow(send.getTemplateId());
            Map<String, Object> params = fromJsonObjectMap(send.getQueryParametersJson());
            ResolutionOutcome refreshed = resolveRecipientsForMode(template.getActiveVersion(), params, null, null);

            boolean introducesNewExternal = refreshed.recipients().stream()
                    .filter(r -> allowlistPort.classifyEmail(r.email()) == RecipientClassification.EXTERNAL)
                    .anyMatch(r -> !previouslyApprovedExternals.contains(r.email().toLowerCase(Locale.ROOT)));

            if (introducesNewExternal) {
                send.setStatus(EmailSendStatus.PENDING_REVIEW);
                sendRepository.save(send);
                audit(sendId, AppConstants.AUDIT_RE_APPROVAL_REQUIRED, "SYSTEM",
                        "Query refresh at dispatch introduced new external recipient(s) not in original approval.");
                return;
            }

            sendRecipientRepository.deleteAll(sendRecipientRepository.findByEmailSend_SendIdOrderBySendRecipientIdAsc(sendId));
            int extCount = 0;
            for (ResolvedRecipient r : refreshed.recipients()) {
                RecipientClassification cls = allowlistPort.classifyEmail(r.email());
                if (cls == RecipientClassification.EXTERNAL) extCount++;
                sendRecipientRepository.save(EmailSendRecipient.builder()
                        .emailSend(send)
                        .recipientName(r.name())
                        .recipientEmail(r.email())
                        .classification(cls)
                        .mergeValuesJson(toJson(r.mergeValues()))
                        .deliveryStatus(RecipientDeliveryStatus.PENDING)
                        .bounceCategory(BounceCategory.NONE)
                        .retryCount(0)
                        .flaggedForContactReview(false)
                        .build());
            }
            send.setTotalRecipients(refreshed.recipients().size());
            send.setExternalRecipients(extCount);
        }

        send.setStatus(EmailSendStatus.IN_PROGRESS);
        sendRepository.save(send);
    }

    
    public void recordDeliveryCallback(Long sendId, DispatchResultCallbackRequest callback) {
        EmailSend send = getSendOrThrow(sendId);
        EmailSendRecipient recipient = sendRecipientRepository.findById(callback.getSendRecipientId())
                .orElseThrow(() -> new EntityNotFoundException(AppConstants.MSG_SEND_RECIPIENT_NOT_FOUND));

        if (callback.isDelivered()) {
            recipient.setDeliveryStatus(RecipientDeliveryStatus.DELIVERED);
            recipient.setBounceCategory(BounceCategory.NONE);
            recipient.setDeliveredAt(OffsetDateTime.now());
            send.setDeliveredCount(send.getDeliveredCount() + 1);
        } else if (callback.getBounceCategory() == BounceCategory.TEMPORARY) {
            recipient.setDeliveryStatus(RecipientDeliveryStatus.RETRYING);
            recipient.setBounceCategory(BounceCategory.TEMPORARY);
            recipient.setRetryCount(recipient.getRetryCount() + 1);
            recipient.setFailureReason(callback.getFailureReason());
        } else {
            recipient.setDeliveryStatus(RecipientDeliveryStatus.FAILED_PERMANENT);
            recipient.setBounceCategory(BounceCategory.PERMANENT);
            recipient.setFailureReason(callback.getFailureReason());
            send.setFailedCount(send.getFailedCount() + 1);

            long priorPermanentFailures = sendRecipientRepository
                    .countByRecipientEmailIgnoreCaseAndBounceCategory(recipient.getRecipientEmail(), BounceCategory.PERMANENT);
            if (priorPermanentFailures + 1 >= AppConstants.PERMANENT_BOUNCE_THRESHOLD) {
                recipient.setFlaggedForContactReview(true);
            }
        }
        sendRecipientRepository.save(recipient);

        if (send.getDeliveredCount() + send.getFailedCount() >= send.getTotalRecipients()) {
            send.setStatus(EmailSendStatus.COMPLETED);
            send.setCompletedAt(OffsetDateTime.now());
        }
        sendRepository.save(send);
    }

    // -----------------------------------------------------------------
    // Private Resolution & Formatting Helpers
    // -----------------------------------------------------------------
    private record ResolvedRecipient(Long contactId, String name, String email, Map<String, String> mergeValues) {}
    private record ResolutionOutcome(List<ResolvedRecipient> recipients, List<String> conflictingEmails, List<String> skippedRows) {}

    private ResolutionOutcome resolveRecipientsForMode(
            EmailTemplateVersion version,
            Map<String, Object> queryParameters,
            String uploadSessionToken,
            List<AdHocRecipientRequest> adHocRecipients) {

        List<ResolvedRecipient> rawList = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        switch (version.getRecipientMode()) {
            case CONTACT_DISTRIBUTION_LIST -> {
                for (EmailTemplateRecipientSelection sel : version.getRecipientSelections()) {
                    if (sel.getContactId() != null) {
                        rawList.add(new ResolvedRecipient(sel.getContactId(), "Contact " + sel.getContactId(),
                                "contact" + sel.getContactId() + "@fdic.gov", Map.of()));
                    }
                    if (sel.getDistributionListId() != null) {
                        // Expand active contacts from distribution list port
                    }
                }
            }
            case DATA_SOURCE_QUERY -> {
                // Execute pinned query with supplied parameters via DataSourceQueryPort
                // Map recipientEmailColumn & recipientNameColumn + mergeFields per row
            }
            case FILE_UPLOAD -> {
                if (uploadSessionToken != null && uploadStagingStore.containsKey(uploadSessionToken)) {
                    rawList.addAll(uploadStagingStore.get(uploadSessionToken));
                } else {
                    templateFileRecipientRepository
                            .findByTemplateVersion_TemplateVersionIdOrderByRowNumber(version.getTemplateVersionId())
                            .forEach(row -> {
                                if (Boolean.TRUE.equals(row.getIsValid())) {
                                    rawList.add(new ResolvedRecipient(null, row.getRecipientName(), row.getRecipientEmail(),
                                            Map.of("c_name", row.getRecipientName() != null ? row.getRecipientName() : "")));
                                } else {
                                    skipped.add("Row " + row.getRowNumber() + ": " + row.getValidationError());
                                }
                            });
                }
            }
            case DEFINE_AT_SEND -> {
                if (uploadSessionToken != null && uploadStagingStore.containsKey(uploadSessionToken)) {
                    rawList.addAll(uploadStagingStore.get(uploadSessionToken));
                } else if (adHocRecipients != null) {
                    for (AdHocRecipientRequest req : adHocRecipients) {
                        rawList.add(new ResolvedRecipient(null, req.getRecipientName(), req.getRecipientEmail().trim(),
                                req.getMergeValues() != null ? req.getMergeValues() : Map.of()));
                    }
                }
            }
        }

        Map<String, Map<String, String>> seenByEmail = new HashMap<>();
        Set<String> conflicts = new LinkedHashSet<>();
        List<ResolvedRecipient> deduplicated = new ArrayList<>();

        for (ResolvedRecipient r : rawList) {
            String key = r.email().toLowerCase(Locale.ROOT);
            if (!seenByEmail.containsKey(key)) {
                seenByEmail.put(key, r.mergeValues());
                deduplicated.add(r);
            } else if (!Objects.equals(seenByEmail.get(key), r.mergeValues())) {
                conflicts.add(r.email());
            }
        }

        return new ResolutionOutcome(deduplicated, new ArrayList<>(conflicts), skipped);
    }

    private String renderHtmlWithFirstRow(EmailTemplateVersion version, List<ResolvedRecipient> recipients) {
        String html = version.getBodyHtml();
        if (recipients.isEmpty()) {
            return html;
        }
        return applyMergeFields(html, recipients.get(0).mergeValues());
    }

    private String applyMergeFields(String html, Map<String, String> values) {
        if (html == null || values == null) return html;
        String result = html;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    private EmailTemplate getActiveTemplateOrThrow(Long templateId) {
        EmailTemplate template = templateRepository.findByTemplateIdAndDeletedAtIsNull(templateId)
                .orElseThrow(() -> new EntityNotFoundException(EmailTemplateConstants.MSG_TEMPLATE_NOT_FOUND));
        if (template.getStatus() != TemplateStatus.ACTIVE || template.getActiveVersion() == null) {
            throw new IllegalStateException(AppConstants.MSG_TEMPLATE_NOT_ACTIVE);
        }
        return template;
    }

    private EmailSend getSendOrThrow(Long sendId) {
        return sendRepository.findById(sendId)
                .orElseThrow(() -> new EntityNotFoundException(AppConstants.MSG_SEND_NOT_FOUND));
    }

    private EmailSendSummaryResponse toSummaryResponse(EmailSend s) {
        return EmailSendSummaryResponse.builder()
                .sendId(s.getSendId())
                .templateId(s.getTemplateId())
                .templateName(s.getTemplateName())
                .totalRecipients(s.getTotalRecipients())
                .externalRecipients(s.getExternalRecipients())
                .status(s.getStatus())
                .submittedBy(s.getSubmittedBy())
                .submittedAt(s.getSubmittedAt())
                .completedAt(s.getCompletedAt())
                .build();
    }

    private EmailSendRecipientResponse toRecipientResponse(EmailSendRecipient r) {
        return EmailSendRecipientResponse.builder()
                .sendRecipientId(r.getSendRecipientId())
                .recipientName(r.getRecipientName())
                .recipientEmail(r.getRecipientEmail())
                .classification(r.getClassification())
                .deliveryStatus(r.getDeliveryStatus())
                .bounceCategory(r.getBounceCategory())
                .failureReason(r.getFailureReason())
                .flaggedForContactReview(r.isFlaggedForContactReview())
                .deliveredAt(r.getDeliveredAt())
                .build();
    }

    private void audit(Long sendId, String action, String user, String detail) {
        sendAuditLogRepository.save(EmailSendAuditLog.builder()
                .sendId(sendId)
                .action(action)
                .performedBy(user)
                .detail(detail)
                .build());
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to read uploaded file.", e);
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failure", e);
        }
    }

    private Map<String, String> fromJsonMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private Map<String, Object> fromJsonObjectMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}