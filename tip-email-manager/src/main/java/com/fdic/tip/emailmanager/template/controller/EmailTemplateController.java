package com.fdic.tip.emailmanager.template.controller;

import com.fdic.tip.emailmanager.template.dto.*;
import com.fdic.tip.emailmanager.template.enums.ChangeType;
import com.fdic.tip.emailmanager.template.service.EmailTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author/edit/lifecycle/history/approval endpoints for email templates
 * (EM-8 through EM-13).
 *
 * Authorization: coarse "can this role hit this endpoint at all" rules
 * (e.g. Analysts never reach /change-requests/**) are enforced centrally
 * via SecurityFilterChain URL rules in SecurityConfig — not
 * @PreAuthorize. Finer scoping that depends on *which* rows a role may
 * see (Analysts limited to Active templates / active-version-only
 * history) is applied in the service layer against the caller's
 * granted authorities, passed through as roles() below.
 */
@RestController
@RequestMapping("/api/v1/email-manager/templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    // ---- EM-8: Author wizard ----
    @PostMapping
    public ResponseEntity<EmailTemplateDetailResponse> createDraft(
            @Valid @RequestBody TemplateDetailsRequest request,
            @AuthenticationPrincipal String currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(emailTemplateService.createDraft(request, currentUser));
    }

    @PutMapping("/{templateId}/content")
    public ResponseEntity<EmailTemplateDetailResponse> updateContent(
            @PathVariable Long templateId,
            @Valid @RequestBody EmailContentRequest request,
            @AuthenticationPrincipal String currentUser) {
        return ResponseEntity.ok(emailTemplateService.updateContent(templateId, request, currentUser));
    }

    @PostMapping(value = "/{templateId}/attachments", consumes = "multipart/form-data")
    public ResponseEntity<EmailTemplateDetailResponse> addAttachment(
            @PathVariable Long templateId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String currentUser) {
        return ResponseEntity.ok(emailTemplateService.addAttachment(templateId, file, currentUser));
    }

    @PutMapping("/{templateId}/recipients")
    public ResponseEntity<EmailTemplateDetailResponse> updateRecipients(
            @PathVariable Long templateId,
            @Valid @RequestBody RecipientsRequest request,
            @AuthenticationPrincipal String currentUser) {
        return ResponseEntity.ok(emailTemplateService.updateRecipients(templateId, request, currentUser));
    }

    @PostMapping("/{templateId}/adopt-latest-query-version")
    public ResponseEntity<EmailTemplateDetailResponse> adoptLatestQueryVersion(
            @PathVariable Long templateId,
            @AuthenticationPrincipal String currentUser) {
        return ResponseEntity.ok(emailTemplateService.adoptLatestQueryVersion(templateId, currentUser));
    }

    @PostMapping("/{templateId}/submit")
    public ResponseEntity<EmailTemplateDetailResponse> submitForApproval(
            @PathVariable Long templateId,
            @Valid @RequestBody SubmitForApprovalRequest request,
            @AuthenticationPrincipal String currentUser) {
        return ResponseEntity.ok(emailTemplateService.submitForApproval(templateId, request, currentUser));
    }

    // ---- EM-9: Edit ----
    @PostMapping("/{templateId}/edit")
    public ResponseEntity<EmailTemplateDetailResponse> startEdit(
            @PathVariable Long templateId,
            @AuthenticationPrincipal String currentUser) {
        return ResponseEntity.ok(emailTemplateService.startEdit(templateId, currentUser));
    }

    // ---- EM-10: Retire / Reactivate ----
    @PostMapping("/{templateId}/retire")
    public ResponseEntity<ChangeRequestResponse> retire(
            @PathVariable Long templateId,
            @Valid @RequestBody LifecycleRequest request,
            @AuthenticationPrincipal String currentUser) {
        return ResponseEntity.ok(emailTemplateService.retire(templateId, request, currentUser));
    }

    @PostMapping("/{templateId}/reactivate")
    public ResponseEntity<ChangeRequestResponse> reactivate(
            @PathVariable Long templateId,
            @Valid @RequestBody LifecycleRequest request,
            @AuthenticationPrincipal String currentUser) {
        return ResponseEntity.ok(emailTemplateService.reactivate(templateId, request, currentUser));
    }

    // ---- EM-12: Restore ----
    @PostMapping("/{templateId}/restore")
    public ResponseEntity<ChangeRequestResponse> restore(
            @PathVariable Long templateId,
            @Valid @RequestBody RestoreRequest request,
            @AuthenticationPrincipal String currentUser) {
        return ResponseEntity.ok(emailTemplateService.restore(templateId, request, currentUser));
    }

    // ---- EM-11: History / compare ----
    @GetMapping("/{templateId}/versions")
    public ResponseEntity<List<VersionHistoryEntryResponse>> getVersionHistory(
            @PathVariable Long templateId, Authentication authentication) {
        return ResponseEntity.ok(emailTemplateService.getVersionHistory(templateId, roles(authentication)));
    }

    @GetMapping("/{templateId}/versions/compare")
    public ResponseEntity<VersionCompareResponse> compareVersions(
            @PathVariable Long templateId,
            @RequestParam Long versionAId,
            @RequestParam Long versionBId,
            Authentication authentication) {
        return ResponseEntity.ok(emailTemplateService.compareVersions(templateId, versionAId, versionBId, roles(authentication)));
    }

    // ---- EM-13: Approval queue ----
    @GetMapping("/change-requests")
    public ResponseEntity<Page<ChangeRequestResponse>> getApprovalQueue(
            @RequestParam(required = false) ChangeType changeType, Pageable pageable) {
        return ResponseEntity.ok(emailTemplateService.getApprovalQueue(changeType, pageable));
    }

    @PostMapping("/change-requests/{changeRequestId}/approve")
    public ResponseEntity<ChangeRequestResponse> approve(
            @PathVariable Long changeRequestId,
            @Valid @RequestBody ApprovalDecisionRequest request,
            @AuthenticationPrincipal String currentUser) {
        return ResponseEntity.ok(emailTemplateService.approve(changeRequestId, request, currentUser));
    }

    @PostMapping("/change-requests/{changeRequestId}/reject")
    public ResponseEntity<ChangeRequestResponse> reject(
            @PathVariable Long changeRequestId,
            @Valid @RequestBody ApprovalDecisionRequest request,
            @AuthenticationPrincipal String currentUser) {
        return ResponseEntity.ok(emailTemplateService.reject(changeRequestId, request, currentUser));
    }

    @PostMapping("/change-requests/{changeRequestId}/withdraw")
    public ResponseEntity<ChangeRequestResponse> withdraw(
            @PathVariable Long changeRequestId,
            @AuthenticationPrincipal String currentUser,
            Authentication authentication) {
        return ResponseEntity.ok(emailTemplateService.withdraw(changeRequestId, currentUser, roles(authentication)));
    }

    // ---- Ownership / lookup ----
    @PostMapping("/{templateId}/owner")
    public ResponseEntity<EmailTemplateDetailResponse> reassignOwner(
            @PathVariable Long templateId,
            @RequestParam String newOwnerUserId,
            @AuthenticationPrincipal String currentUser) {
        return ResponseEntity.ok(emailTemplateService.reassignOwner(templateId, newOwnerUserId, currentUser));
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<EmailTemplateDetailResponse> getById(@PathVariable Long templateId) {
        return ResponseEntity.ok(emailTemplateService.getById(templateId));
    }

    @GetMapping
    public ResponseEntity<Page<EmailTemplateSummaryResponse>> list(Pageable pageable, Authentication authentication) {
        return ResponseEntity.ok(emailTemplateService.list(pageable, roles(authentication)));
    }

    private Set<String> roles(Authentication authentication) {
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
