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
import gov.fdic.tip.emailmanager.service.ContactService;
import gov.fdic.tip.emailmanager.service.DistributionListService;
import gov.fdic.tip.emailmanager.service.InternalDomainAllowlistService;
import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants;
import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.*;
import gov.fdic.tip.emailmanager.sendemail.dto.*;
import gov.fdic.tip.emailmanager.sendemail.entity.*;
import gov.fdic.tip.emailmanager.sendemail.port.EmailDispatchEnginePort;
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

    // Direct Service Injections
    private final InternalDomainAllowlistService allowlistService;
    private final ContactService contactService;
    private final DistributionListService distributionListService;

    private final EmailDispatchEnginePort dispatchEnginePort;
    private final ObjectMapper objectMapper;

    private final Map<String, List<ResolvedRecipient>> uploadStagingStore = new ConcurrentHashMap<>();

    private static final int PERMANENT_BOUNCE_THRESHOLD = 3;

    // ===================================================================
    // 1. Active Template Selection
    // ===================================================================
    @Override
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
    // 2. Send via Excel Recipient Upload
    // ===================================================================
    @Override
    public SendFileUploadResponse uploadAdHocRecipientFile(Long templateId, MultipartFile file) {
        EmailTemplate template = getActiveTemplateOrThrow(templateId);
        EmailTemplateVersion version = template.getActiveVersion();

        if (version.getRecipientMode() != RecipientMode.FILE_UPLOAD
                && version.getRecipientMode() != RecipientMode.DEFINE_AT_SEND) {
            throw new IllegalStateException(AppConstants.MSG_FILE_UPLOAD_NOT_ALLOWED);
        }

        // Implementation stays the same using file parser utility...
        return null; // Keep your existing file parsing logic here
    }

    // ===================================================================
    // 3. Preview Send
    // ===================================================================
    @Override
    @Transactional(readOnly = true)
    public SendPreviewResponse previewSend(SendPreviewRequest request) {
        EmailTemplate template = getActiveTemplateOrThrow(request.getTemplateId());
        EmailTemplateVersion version = template.getActiveVersion();

        ResolutionOutcome resolution = resolveRecipientsForMode(
                version, request.getQueryParameters(), request.getUploadSessionToken(), request.getAdHocRecipients());

        String renderedSampleHtml = renderHtmlWithFirstRow(version, resolution.recipients());

        int externalCount = (int) resolution.recipients().stream()
                .filter(r -> classifyEmail(r.email()) == RecipientClassification.EXTERNAL)
                .count();
        int totalCount = resolution.recipients().size();

        List<EmailSendRecipientResponse> recipientDtos = resolution.recipients().stream()
                .map(r -> EmailSendRecipientResponse.builder()
                        .recipientName(r.name())
                        .recipientEmail(r.email())
                        .classification(classifyEmail(r.email()))
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
    // 4. Submit Send
    // ===================================================================
    @Override
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
                .filter(r -> classifyEmail(r.email()) == RecipientClassification.EXTERNAL)
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
                    .classification(classifyEmail(r.email()))
                    .mergeValuesJson(toJson(r.mergeValues()))
                    .deliveryStatus(RecipientDeliveryStatus.PENDING)
                    .bounceCategory(BounceCategory.NONE)
                    .retryCount(0)
                    .flaggedForContactReview(false)
                    .build());
        }

        if (initialStatus == EmailSendStatus.QUEUED) {
            dispatchEnginePort.enqueueSend(send.getSendId());
        }

        return getSendDetail(send.getSendId());
    }

    // ===================================================================
    // Helper: Domain Classification using InternalDomainAllowlistService directly
    // ===================================================================
    private RecipientClassification classifyEmail(String emailAddress) {
        if (emailAddress == null || !emailAddress.contains("@")) {
            return RecipientClassification.EXTERNAL;
        }
        String emailDomain = emailAddress.substring(emailAddress.lastIndexOf('@') + 1)
                .trim()
                .toLowerCase(Locale.ROOT);

        boolean isInternal = allowlistService.getAllDomains().stream()
                .filter(d -> d.getDomain() != null)
                .filter(d -> !AppConstants.STATUS_INACTIVE.equalsIgnoreCase(d.getStatus()))
                .anyMatch(d -> d.getDomain().trim().toLowerCase(Locale.ROOT).equals(emailDomain));

        return isInternal ? RecipientClassification.INTERNAL : RecipientClassification.EXTERNAL;
    }

    // ===================================================================
    // 5. Cancel Send, History, Approvals & Callbacks
    // ===================================================================
    @Override
    public EmailSendDetailResponse cancelSend(Long sendId) {
        String currentUser = CurrentUserInfo.getCurrentUserEmail();
        EmailSend send = getSendOrThrow(sendId);
        if (!send.getSubmittedBy().equals(currentUser)) {
            throw new SecurityException(AppConstants.MSG_CANCEL_UNAUTHORIZED);
        }
        send.setStatus(EmailSendStatus.CANCELED);
        send.setCanceledAt(OffsetDateTime.now());
        sendRepository.save(send);
        return getSendDetail(sendId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailSendSummaryResponse> getMySends(Pageable pageable) {
        return sendRepository.findBySubmittedByOrderBySubmittedAtDesc(CurrentUserInfo.getCurrentUserEmail(), pageable)
                .map(this::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EmailSendDetailResponse getSendDetail(Long sendId) {
        EmailSend send = getSendOrThrow(sendId);
        List<EmailSendRecipient> recipients = sendRecipientRepository.findByEmailSend_SendIdOrderBySendRecipientIdAsc(sendId);
        return EmailSendDetailResponse.builder()
                .sendId(send.getSendId())
                .templateName(send.getTemplateName())
                .status(send.getStatus())
                .totalRecipients(send.getTotalRecipients())
                .externalRecipients(send.getExternalRecipients())
                .recipients(recipients.stream().map(this::toRecipientResponse).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportFailedDeliveriesCsv(Long sendId) {
        // CSV export logic...
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailSendSummaryResponse> getExternalApprovalQueue(Pageable pageable) {
        return sendRepository.findByStatusAndExternalRecipientsGreaterThanOrderBySubmittedAtDesc(
                EmailSendStatus.PENDING_REVIEW, 0, pageable).map(this::toSummaryResponse);
    }

    @Override
    public EmailSendDetailResponse decideExternalApproval(Long sendId, ExternalApprovalDecisionRequest request) {
        String currentUser = CurrentUserInfo.getCurrentUserEmail();
        EmailSend send = getSendOrThrow(sendId);
        if (send.getSubmittedBy().equals(currentUser)) {
            throw new SecurityException(AppConstants.MSG_SELF_APPROVAL_BLOCKED);
        }
        send.setReviewedBy(currentUser);
        send.setReviewedAt(OffsetDateTime.now());
        if (Boolean.TRUE.equals(request.getApproved())) {
            send.setStatus(EmailSendStatus.QUEUED);
            dispatchEnginePort.enqueueSend(sendId);
        } else {
            send.setStatus(EmailSendStatus.REJECTED_BY_REVIEWER);
        }
        sendRepository.save(send);
        return getSendDetail(sendId);
    }

    @Override
    public void prepareAndDispatch(Long sendId) {
        // Dispatch logic...
    }

    @Override
    public void recordDeliveryCallback(Long sendId, DispatchResultCallbackRequest callback) {
        // Delivery callback logic...
    }

    // --- Private mapping & resolution helpers ---
    private record ResolvedRecipient(Long contactId, String name, String email, Map<String, String> mergeValues) {}
    private record ResolutionOutcome(List<ResolvedRecipient> recipients, List<String> conflictingEmails, List<String> skippedRows) {}

    private ResolutionOutcome resolveRecipientsForMode(
            EmailTemplateVersion version, 
            Map<String, Object> queryParameters, 
            String token, 
            List<AdHocRecipientRequest> adHocRecipients) {

        List<ResolvedRecipient> rawList = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        switch (version.getRecipientMode()) {
            case CONTACT_DISTRIBUTION_LIST -> {
                if (version.getRecipientSelections() != null) {
                    for (EmailTemplateRecipientSelection sel : version.getRecipientSelections()) {
                        
                        // 1. Resolve Individual Contacts via ContactService
                        if (sel.getContactId() != null) {
                            try {
                                // ContactService provides getAllContacts() or you can find by ID from the list
                                var contactOpt = contactService.getAllContacts().stream()
                                        .filter(c -> c.getId().equals(sel.getContactId()))
                                        .findFirst();
                                        
                                if (contactOpt.isPresent()) {
                                    var contact = contactOpt.get();
                                    if (contact.getEmail() != null && !contact.getEmail().isBlank()) {
                                        String name = contact.getName() != null ? contact.getName() : "";
                                        rawList.add(new ResolvedRecipient(
                                                contact.getId(),
                                                name,
                                                contact.getEmail().trim(),
                                                Map.of("c_name", name)
                                        ));
                                    }
                                } else {
                                    skipped.add("Contact ID " + sel.getContactId() + ": Not found.");
                                }
                            } catch (Exception e) {
                                skipped.add("Contact ID " + sel.getContactId() + ": Failed to resolve contact details.");
                            }
                        }
                        
                        // 2. Expand Distribution Lists via DistributionListService
                        if (sel.getDistributionListId() != null) {
                            try {
                                // DistributionListService uses getDistributionListDetails(id, filter, pageable)
                                var viewDto = distributionListService.getDistributionListDetails(sel.getDistributionListId(), "", Pageable.unpaged());
                                if (viewDto != null && viewDto.getMembers() != null) {
                                    for (var member : viewDto.getMembers()) {
                                        if (member.getEmail() != null && !member.getEmail().isBlank()) {
                                            String memberName = member.getName() != null ? member.getName() : "";
                                            rawList.add(new ResolvedRecipient(
                                                    member.getId(),
                                                    memberName,
                                                    member.getEmail().trim(),
                                                    Map.of("c_name", memberName)
                                            ));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                skipped.add("Distribution List ID " + sel.getDistributionListId() + ": Failed to expand members.");
                            }
                        }
                    }
                }
            }

            case DATA_SOURCE_QUERY -> {
                if (version.getDataSourceQueryId() != null) {
                    try {
                        // If you have a DataSourceQueryService execution method or port, call it here.
                        // Example fetching query metadata/columns or executing:
                        // var queryResultRows = dataSourceQueryService.executeDynamicQuery(version.getDataSourceQueryId(), queryParameters);
                        
                        String emailCol = version.getRecipientEmailColumn() != null ? version.getRecipientEmailColumn() : AppConstants.DEFAULT_EMAIL_COLUMN;
                        String nameCol = version.getRecipientNameColumn() != null ? version.getRecipientNameColumn() : AppConstants.DEFAULT_NAME_COLUMN;

                        // (Populate rawList from query result rows mapping...)
                    } catch (Exception e) {
                        skipped.add("Data Source Query execution failed: " + e.getMessage());
                    }
                }
            }

            case FILE_UPLOAD -> {
                if (token != null && uploadStagingStore.containsKey(token)) {
                    rawList.addAll(uploadStagingStore.get(token));
                } else {
                    templateFileRecipientRepository
                            .findByTemplateVersion_TemplateVersionIdOrderByRowNumber(version.getTemplateVersionId())
                            .forEach(row -> {
                                if (Boolean.TRUE.equals(row.getIsValid())) {
                                    String name = row.getRecipientName() != null ? row.getRecipientName() : "";
                                    rawList.add(new ResolvedRecipient(
                                            null, 
                                            name, 
                                            row.getRecipientEmail().trim(),
                                            Map.of("c_name", name)
                                    ));
                                } else {
                                    skipped.add("Row " + row.getRowNumber() + ": " + row.getValidationError());
                                }
                            });
                }
            }

            case DEFINE_AT_SEND -> {
                if (token != null && uploadStagingStore.containsKey(token)) {
                    rawList.addAll(uploadStagingStore.get(token));
                } else if (adHocRecipients != null) {
                    for (AdHocRecipientRequest req : adHocRecipients) {
                        if (req.getRecipientEmail() != null && req.getRecipientEmail().contains("@")) {
                            String name = req.getRecipientName() != null ? req.getRecipientName() : "";
                            Map<String, String> merges = new HashMap<>();
                            merges.put("c_name", name);
                            if (req.getMergeValues() != null) {
                                merges.putAll(req.getMergeValues());
                            }
                            rawList.add(new ResolvedRecipient(null, name, req.getRecipientEmail().trim(), merges));
                        } else {
                            skipped.add("Ad-hoc recipient: Invalid or missing email address");
                        }
                    }
                }
            }
        }

        // Deduplication & Conflict Detection
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
        return version.getBodyHtml();
    }

    private EmailTemplate getActiveTemplateOrThrow(Long templateId) {
        return templateRepository.findByTemplateIdAndDeletedAtIsNull(templateId)
                .orElseThrow(() -> new EntityNotFoundException(EmailTemplateConstants.MSG_TEMPLATE_NOT_FOUND));
    }

    private EmailSend getSendOrThrow(Long sendId) {
        return sendRepository.findById(sendId)
                .orElseThrow(() -> new EntityNotFoundException(AppConstants.MSG_SEND_NOT_FOUND));
    }

    private EmailSendSummaryResponse toSummaryResponse(EmailSend s) {
        return EmailSendSummaryResponse.builder().sendId(s.getSendId()).status(s.getStatus()).build();
    }

    private EmailSendRecipientResponse toRecipientResponse(EmailSendRecipient r) {
        return EmailSendRecipientResponse.builder().recipientEmail(r.getRecipientEmail()).build();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return null; }
    }
}