package gov.fdic.tip.emailmanager.service.impl;

import gov.fdic.tip.emailmanager.constants.ContactStatus;
import gov.fdic.tip.emailmanager.dto.ApprovedSenderDto;
import gov.fdic.tip.emailmanager.entity.ApprovedSender;
import gov.fdic.tip.emailmanager.repository.ApprovedSenderRepository;
import gov.fdic.tip.emailmanager.repository.InternalDomainAllowlistRepository;
import gov.fdic.tip.emailmanager.service.ApprovedSenderService;
import gov.fdic.tip.emailmanager.service.AuditLogService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApprovedSenderServiceImpl implements ApprovedSenderService {

    private final ApprovedSenderRepository approvedSenderRepository;
    private final InternalDomainAllowlistRepository internalDomainAllowlistRepository;
    private final AuditLogService auditLogService;

    public ApprovedSenderServiceImpl(
            ApprovedSenderRepository approvedSenderRepository,
            InternalDomainAllowlistRepository internalDomainAllowlistRepository,
            AuditLogService auditLogService) {
        this.approvedSenderRepository = approvedSenderRepository;
        this.internalDomainAllowlistRepository = internalDomainAllowlistRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovedSenderDto> getAllSenders() {
        return approvedSenderRepository.findByDeletedAtIsNull().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovedSenderDto> getActiveSenders() {
        return approvedSenderRepository.findByStatusAndDeletedAtIsNull(ContactStatus.ACTIVE).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ApprovedSenderDto createSender(ApprovedSenderDto dto, String username) {
        String normalizedMailbox = normalizeMailboxAddress(dto.getMailboxAddress());

        // Enforce active domain validation rule using InternalDomainAllowlistRepository
        validateInternalDomain(normalizedMailbox);

        // FIX: compare against the normalized address — the unique index on
        // approved_sender is case-insensitive (LOWER(mailbox_address)), so
        // checking the raw, un-normalized value here could pass this check
        // and still collide at the DB layer.
        if (approvedSenderRepository.existsByMailboxAddressAndDeletedAtIsNull(normalizedMailbox)) {
            throw new IllegalArgumentException("Mailbox address already exists.");
        }

        ApprovedSender entity = new ApprovedSender();
        BeanUtils.copyProperties(dto, entity);
        entity.setMailboxAddress(normalizedMailbox);
        entity.setCreatedBy(username);

        ApprovedSender saved = approvedSenderRepository.save(entity);

        auditLogService.logAction(
                "APPROVED_SENDER", 
                saved.getId(), 
                "CREATE", 
                username, 
                "Created approved sender identity: " + saved.getMailboxAddress()
        );

        return convertToDto(saved);
    }

    @Override
    public ApprovedSenderDto updateSender(Long id, ApprovedSenderDto dto, String username) {
        ApprovedSender entity = approvedSenderRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Sender identity not found with id: " + id));

        String normalizedMailbox = normalizeMailboxAddress(dto.getMailboxAddress());

        // Enforce active domain validation rule using InternalDomainAllowlistRepository
        validateInternalDomain(normalizedMailbox);

        // FIX: createSender rejected a duplicate mailbox address, but this
        // method previously had no equivalent check — updating a sender's
        // address to one already in use fell through to a raw DB
        // constraint violation instead of a clean validation error.
        // NOTE: requires adding
        // existsByMailboxAddressAndDeletedAtIsNullAndIdNot(String, Long)
        // to ApprovedSenderRepository, mirroring the pattern already used
        // by ContactAttributeRepository.existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull.
        if (approvedSenderRepository.existsByMailboxAddressAndDeletedAtIsNullAndIdNot(normalizedMailbox, id)) {
            throw new IllegalArgumentException("Mailbox address already exists.");
        }

        entity.setDisplayName(dto.getDisplayName());
        entity.setMailboxAddress(normalizedMailbox);
        entity.setStatus(dto.getStatus());
        entity.setUpdatedBy(username);

        ApprovedSender updated = approvedSenderRepository.save(entity);

        auditLogService.logAction(
                "APPROVED_SENDER", 
                updated.getId(), 
                "UPDATE", 
                username, 
                "Updated sender status to " + updated.getStatus()
        );

        return convertToDto(updated);
    }

    @Override
    public void deleteSender(Long id, String username) {
        ApprovedSender entity = approvedSenderRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Sender identity not found with id: " + id));

        // FIX: previously only deletedBy/deletedAt were set, leaving
        // status = ACTIVE on a deactivated row — anything that filters
        // on status alone (rather than ANDing deletedAt IS NULL) would
        // have shown a stale active sender.
        entity.setStatus(ContactStatus.INACTIVE);
        entity.setDeletedBy(username);
        entity.setDeletedAt(OffsetDateTime.now());
        approvedSenderRepository.save(entity);

        auditLogService.logAction(
                "APPROVED_SENDER", 
                id, 
                "DELETE", 
                username, 
                "Deleted sender identity"
        );
    }

    /**
     * Lowercases and trims the mailbox address so it matches how
     * ContactServiceImpl normalizes contact emails, and how the DB's
     * unique index (LOWER(mailbox_address)) compares them.
     */
    private String normalizeMailboxAddress(String mailboxAddress) {
        if (mailboxAddress == null || mailboxAddress.isBlank()) {
            throw new IllegalArgumentException("Mailbox address is required.");
        }
        return mailboxAddress.toLowerCase().trim();
    }

    /**
     * Extracts domain from the email and checks InternalDomainAllowlistRepository for active status.
     */
    private void validateInternalDomain(String emailAddress) {
        if (emailAddress == null || !emailAddress.contains("@")) {
            throw new IllegalArgumentException("Invalid email address format.");
        }

        String domain = emailAddress.substring(emailAddress.indexOf("@") + 1).toLowerCase().trim();
        
        boolean isDomainAllowedAndActive = internalDomainAllowlistRepository
                .existsByDomainAndStatusAndDeletedAtIsNull(domain, ContactStatus.ACTIVE);

        if (!isDomainAllowedAndActive) {
            throw new IllegalArgumentException("Only active internal domain email address are permitted.");
        }
    }

    private ApprovedSenderDto convertToDto(ApprovedSender entity) {
        ApprovedSenderDto dto = new ApprovedSenderDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}