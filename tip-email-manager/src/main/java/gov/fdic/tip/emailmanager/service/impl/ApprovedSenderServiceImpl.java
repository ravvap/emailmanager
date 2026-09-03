package gov.fdic.tip.emailmanager.service.impl;

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
        return approvedSenderRepository.findByStatusAndDeletedAtIsNull("ACTIVE").stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ApprovedSenderDto createSender(ApprovedSenderDto dto, String username) {
        // Enforce active domain validation rule using InternalDomainAllowlistRepository
        validateInternalDomain(dto.getMailboxAddress());

        if (approvedSenderRepository.existsByMailboxAddressAndDeletedAtIsNull(dto.getMailboxAddress())) {
            throw new IllegalArgumentException("Mailbox address already exists.");
        }

        ApprovedSender entity = new ApprovedSender();
        BeanUtils.copyProperties(dto, entity);
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

        // Enforce active domain validation rule using InternalDomainAllowlistRepository
        validateInternalDomain(dto.getMailboxAddress());

        entity.setDisplayName(dto.getDisplayName());
        entity.setMailboxAddress(dto.getMailboxAddress());
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
     * Extracts domain from the email and checks InternalDomainAllowlistRepository for active status.
     */
    private void validateInternalDomain(String emailAddress) {
        if (emailAddress == null || !emailAddress.contains("@")) {
            throw new IllegalArgumentException("Invalid email address format.");
        }

        String domain = emailAddress.substring(emailAddress.indexOf("@") + 1).toLowerCase().trim();
        
        boolean isDomainAllowedAndActive = internalDomainAllowlistRepository
                .existsByDomainAndStatusAndDeletedAtIsNull(domain, "ACTIVE");

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