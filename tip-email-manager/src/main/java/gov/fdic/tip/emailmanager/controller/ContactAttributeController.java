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

import gov.fdic.tip.emailmanager.dto.ContactAttributeDto;
import gov.fdic.tip.emailmanager.service.ContactAttributeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/contact-attributes")
public class ContactAttributeController {

    private final ContactAttributeService service;

    public ContactAttributeController(ContactAttributeService service) {
        this.service = service;
    }

    // GET all attributes -> VIEW permission
    @GetMapping
    @PreAuthorize(AppConstants.PERM_CONTACT_ATTRIBUTE_VIEW)
    public ResponseEntity<List<ContactAttributeDto>> getAllAttributes() {
        return ResponseEntity.ok(service.getAllAttributes());
    }

    // GET active attributes -> VIEW permission
    @GetMapping("/active")
    @PreAuthorize(AppConstants.PERM_CONTACT_ATTRIBUTE_VIEW)
    public ResponseEntity<List<ContactAttributeDto>> getActiveAttributes() {
        return ResponseEntity.ok(service.getActiveAttributes());
    }

    // POST create attribute -> ADD permission
    @PostMapping
    @PreAuthorize(AppConstants.PERM_CONTACT_ATTRIBUTE_ADD)
    public ResponseEntity<ContactAttributeDto> createAttribute(
            @Valid @RequestBody ContactAttributeDto dto,
            Authentication authentication) {
        ContactAttributeDto created = service.createAttribute(dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT update attribute -> EDIT permission
    @PutMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_CONTACT_ATTRIBUTE_EDIT)
    public ResponseEntity<ContactAttributeDto> updateAttribute(
            @PathVariable Long id,
            @Valid @RequestBody ContactAttributeDto dto,
            Authentication authentication) {
        ContactAttributeDto updated = service.updateAttribute(id, dto, authentication.getName());
        return ResponseEntity.ok(updated);
    }

    // DELETE attribute -> DELETE permission
    @DeleteMapping("/{id}")
    @PreAuthorize(AppConstants.PERM_CONTACT_ATTRIBUTE_DELETE)
    public ResponseEntity<Void> deleteAttribute(
            @PathVariable Long id,
            Authentication authentication) {
        service.deleteAttribute(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}