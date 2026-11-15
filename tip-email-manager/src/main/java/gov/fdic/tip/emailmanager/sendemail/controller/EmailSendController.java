package gov.fdic.tip.emailmanager.sendemail.controller;

import com.fdic.tip.emailmanager.template.dto.EmailTemplateSummaryResponse;
import gov.fdic.tip.emailmanager.sendemail.dto.*;
import gov.fdic.tip.emailmanager.sendemail.service.EmailSendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/email-manager/sends")
@RequiredArgsConstructor
public class EmailSendController {

    private final EmailSendService emailSendService;

    // ---- Template dropdown (Active, approved template versions only) ----
    @GetMapping("/templates")
    public ResponseEntity<List<EmailTemplateSummaryResponse>> getActiveTemplates() {
        return ResponseEntity.ok(emailSendService.getActiveApprovedTemplates());
    }

    // ---- Ad-hoc Excel recipient upload at send time ----
    @PostMapping(value = "/templates/{templateId}/upload-recipients", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SendFileUploadResponse> uploadRecipientFile(
            @PathVariable Long templateId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(emailSendService.uploadAdHocRecipientFile(templateId, file));
    }

    // ---- Preview send (renders 1st row sample, classifies internal/external, checks conflicts) ----
    @PostMapping("/preview")
    public ResponseEntity<SendPreviewResponse> preview(
            @Valid @RequestBody SendPreviewRequest request) {
        return ResponseEntity.ok(emailSendService.previewSend(request));
    }

    // ---- Submit send (freezes content, queues internal or routes external to Pending Review) ----
    @PostMapping
    public ResponseEntity<EmailSendDetailResponse> submit(
            @Valid @RequestBody SubmitEmailSendRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(emailSendService.submitSend(request));
    }

    // ---- Cancel send (before reviewer decision or before dispatch worker pickup) ----
    @PostMapping("/{sendId}/cancel")
    public ResponseEntity<EmailSendDetailResponse> cancel(@PathVariable Long sendId) {
        return ResponseEntity.ok(emailSendService.cancelSend(sendId));
    }

    // ---- Send History & Detail Drill-down ----
    @GetMapping
    public ResponseEntity<Page<EmailSendSummaryResponse>> getMySends(Pageable pageable) {
        return ResponseEntity.ok(emailSendService.getMySends(pageable));
    }

    @GetMapping("/{sendId}")
    public ResponseEntity<EmailSendDetailResponse> getSendDetail(@PathVariable Long sendId) {
        return ResponseEntity.ok(emailSendService.getSendDetail(sendId));
    }

    // ---- Export Failed Deliveries CSV (Completed sends only) ----
    @GetMapping("/{sendId}/failed-recipients/export")
    public ResponseEntity<byte[]> exportFailedDeliveries(@PathVariable Long sendId) {
        byte[] csv = emailSendService.exportFailedDeliveriesCsv(sendId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"failed-deliveries-" + sendId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // ---- EM-15: External-Recipient Approval Queue ----
    @GetMapping("/external-approvals")
    public ResponseEntity<Page<EmailSendSummaryResponse>> getExternalApprovalQueue(Pageable pageable) {
        return ResponseEntity.ok(emailSendService.getExternalApprovalQueue(pageable));
    }

    @PostMapping("/external-approvals/{sendId}/decision")
    public ResponseEntity<EmailSendDetailResponse> decideExternalApproval(
            @PathVariable Long sendId,
            @Valid @RequestBody ExternalApprovalDecisionRequest request) {
        return ResponseEntity.ok(emailSendService.decideExternalApproval(sendId, request));
    }

    // ---- Internal Dispatch Engine Callback ----
    @PostMapping("/{sendId}/delivery-callback")
    public ResponseEntity<Void> recordDeliveryCallback(
            @PathVariable Long sendId,
            @Valid @RequestBody DispatchResultCallbackRequest callback) {
        emailSendService.recordDeliveryCallback(sendId, callback);
        return ResponseEntity.ok().build();
    }
}