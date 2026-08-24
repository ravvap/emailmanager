package com.fdic.tip.emailmanager.controller;

import com.fdic.tip.emailmanager.dto.NoReplyMailboxDto;
import com.fdic.tip.emailmanager.service.NoReplyMailboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/no-reply-mailbox")
@RequiredArgsConstructor
@Tag(name = "No-Reply Mailbox Controller", description = "Endpoint for managing single system-wide no-reply mailbox with Azure integration")
public class NoReplyMailboxController {

    private final NoReplyMailboxService service;

    @GetMapping
    @Operation(summary = "Get current system-wide no-reply mailbox configuration")
    public ResponseEntity<NoReplyMailboxDto> getMailbox() {
        return ResponseEntity.ok(service.getMailbox());
    }

    @PutMapping
    @Operation(summary = "Configure or update system-wide no-reply mailbox in Azure Cloud")
    public ResponseEntity<NoReplyMailboxDto> saveOrUpdateMailbox(
            @Valid @RequestBody NoReplyMailboxDto dto,
            @RequestHeader(value = "X-User-Name", defaultValue = "SYSTEM") String username) {
        return ResponseEntity.ok(service.saveOrUpdateMailbox(dto, username));
    }

    @DeleteMapping
    @Operation(summary = "Soft delete current no-reply mailbox configuration and disable Azure binding")
    public ResponseEntity<Void> deleteMailbox(
            @RequestHeader(value = "X-User-Name", defaultValue = "SYSTEM") String username) {
        service.deleteMailbox(username);
        return ResponseEntity.noContent().build();
    }
}