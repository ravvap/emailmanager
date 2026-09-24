package com.fdic.tip.emailmanager.template.service;

import com.fdic.tip.emailmanager.template.dto.*;
import com.fdic.tip.emailmanager.template.enums.ChangeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public interface EmailTemplateService {

    // ---- EM-8: Author (wizard steps on the current draft version) ----
    EmailTemplateDetailResponse createDraft(TemplateDetailsRequest request, String currentUser);

    EmailTemplateDetailResponse updateContent(Long templateId, EmailContentRequest request, String currentUser);

    EmailTemplateDetailResponse updateRecipients(Long templateId, RecipientsRequest request, String currentUser);

    EmailTemplateDetailResponse addAttachment(Long templateId, MultipartFile file, String currentUser);

    /** Adopt the latest approved data source query version onto the current draft, re-flagging any broken mappings (EM-9 AC). */
    EmailTemplateDetailResponse adoptLatestQueryVersion(Long templateId, String currentUser);

    /** Submits the current draft as a NEW_TEMPLATE or EDIT change request (EM-8 step 4 / EM-9). Blocks on unresolved conflicts. */
    EmailTemplateDetailResponse submitForApproval(Long templateId, SubmitForApprovalRequest request, String currentUser);

    // ---- EM-9: Edit an existing (active) template ----
    /** Creates the next draft version, pre-filled from the current active version. Only one pending draft per template at a time. */
    EmailTemplateDetailResponse startEdit(Long templateId, String currentUser);

    // ---- EM-10: Retire / Reactivate ----
    ChangeRequestResponse retire(Long templateId, LifecycleRequest request, String currentUser);

    ChangeRequestResponse reactivate(Long templateId, LifecycleRequest request, String currentUser);

    // ---- EM-11: Version history / compare ----
    /** roles drives Analyst scoping: Analysts see history of the active version only. */
    List<VersionHistoryEntryResponse> getVersionHistory(Long templateId, Set<String> roles);

    VersionCompareResponse compareVersions(Long templateId, Long versionAId, Long versionBId, Set<String> roles);

    // ---- EM-12: Restore ----
    /** Copies a prior version's content exactly into a new draft and immediately submits it as a RESTORE change request. */
    ChangeRequestResponse restore(Long templateId, RestoreRequest request, String currentUser);

    // ---- EM-13: Approval queue (all change types) ----
    Page<ChangeRequestResponse> getApprovalQueue(ChangeType changeTypeFilter, Pageable pageable);

    ChangeRequestResponse approve(Long changeRequestId, ApprovalDecisionRequest request, String currentUser);

    ChangeRequestResponse reject(Long changeRequestId, ApprovalDecisionRequest request, String currentUser);

    /** Submitter, or a Manager/Admin on the submitter's behalf, withdraws a pending request. */
    ChangeRequestResponse withdraw(Long changeRequestId, String currentUser, Set<String> roles);

    // ---- Ownership / lookup ----
    EmailTemplateDetailResponse reassignOwner(Long templateId, String newOwnerUserId, String currentUser);

    EmailTemplateDetailResponse getById(Long templateId);

    /** roles drives Analyst scoping: Analysts only see ACTIVE templates. */
    Page<EmailTemplateSummaryResponse> list(Pageable pageable, Set<String> roles);
}
