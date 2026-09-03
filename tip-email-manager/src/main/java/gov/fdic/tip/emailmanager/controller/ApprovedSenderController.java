package gov.fdic.tip.emailmanager.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fdic.tip.emailmanager.constant.AppConstants;

import gov.fdic.tip.emailmanager.dto.ApprovedSenderDto;
import gov.fdic.tip.emailmanager.service.ApprovedSenderService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/approved-senders")
public class ApprovedSenderController {

    private final ApprovedSenderService service;

    public ApprovedSenderController(ApprovedSenderService service) {
        this.service = service;
    }

    // GET -> VIEW permission
    @GetMapping
    @PreAuthorize(AppConstants.PERM_APPROVED_SENDER_VIEW)
    public ResponseEntity<List<ApprovedSenderDto>> getAllSenders() {
        return ResponseEntity.ok(service.getAllSenders());
    }

    // GET Active -> VIEW permission
    @GetMapping("/active")
    @PreAuthorize(AppConstants.PERM_APPROVED_SENDER_VIEW)
    public ResponseEntity<List<ApprovedSenderDto>> getActiveSenders() {
        return ResponseEntity.ok(service.getActiveSenders());
    }

    // POST -> ADD permission
    @PostMapping
    @PreAuthorize(AppConstants.PERM_APPROVED_SENDER_ADD)
    public ResponseEntity<ApprovedSenderDto> createSender(
            @Valid @RequestBody ApprovedSenderDto dto,
            Authentication authentication) {
        ApprovedSenderDto created = service.createSender(dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT -> EDIT permission
    @PutMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_APPROVED_SENDER_EDIT)
    public ResponseEntity<ApprovedSenderDto> updateSender(
            @PathVariable Long id,
            @Valid @RequestBody ApprovedSenderDto dto,
            Authentication authentication) {
        ApprovedSenderDto updated = service.updateSender(id, dto, authentication.getName());
        return ResponseEntity.ok(updated);
    }

    // DELETE -> DELETE permission
    @DeleteMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_APPROVED_SENDER_DELETE)
    public ResponseEntity<Void> deleteSender(
            @PathVariable Long id,
            Authentication authentication) {
        service.deleteSender(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}