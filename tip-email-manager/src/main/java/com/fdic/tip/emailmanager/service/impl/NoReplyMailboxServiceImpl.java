package com.fdic.tip.emailmanager.service.impl;

 import java.time.ZonedDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fdic.tip.emailmanager.constant.AppConstants;
import com.fdic.tip.emailmanager.dto.NoReplyMailboxDto;
import com.fdic.tip.emailmanager.entity.NoReplyMailbox;
import com.fdic.tip.emailmanager.mapper.NoReplyMailboxMapper;
import com.fdic.tip.emailmanager.repository.NoReplyMailboxRepository;
import com.fdic.tip.emailmanager.service.AzureMailboxConfigService;
import com.fdic.tip.emailmanager.service.NoReplyMailboxService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoReplyMailboxServiceImpl implements NoReplyMailboxService {

    private static final Long SINGLETON_ID = 1L;

    private final NoReplyMailboxRepository repository;
     private final NoReplyMailboxMapper mapper;
    private final AzureMailboxConfigService azureMailboxConfigService;

    @Override
    public NoReplyMailboxDto getMailbox() {
        return repository.findByIdAndDeletedAtIsNull(SINGLETON_ID)
                .map(mapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("No-Reply Mailbox configuration not found."));
    }

    @Override
    @Transactional
    public NoReplyMailboxDto saveOrUpdateMailbox(NoReplyMailboxDto dto, String username) {
        if (repository.existsByEmailAddressAndIdNotAndDeletedAtIsNull(dto.getEmailAddress(), SINGLETON_ID)) {
            throw new IllegalArgumentException("Email address is already in use: " + dto.getEmailAddress());
        }

        // Configure mailbox settings in Azure Cloud
        azureMailboxConfigService.configureAzureMailbox(dto.getEmailAddress());

        boolean exists = repository.existsById(SINGLETON_ID);

        NoReplyMailbox mailbox = repository.findByIdAndDeletedAtIsNull(SINGLETON_ID)
                .orElseGet(() -> {
                    NoReplyMailbox newEntity = new NoReplyMailbox();
                    newEntity.setId(SINGLETON_ID);
                    newEntity.setCreatedBy(username);
                    newEntity.setCreatedAt(ZonedDateTime.now());
                    return newEntity;
                });

        mapper.updateEntityFromDto(dto, mailbox);
        mailbox.setUpdatedBy(username);
        mailbox.setUpdatedAt(ZonedDateTime.now());
        mailbox.setDeletedBy(null);
        mailbox.setDeletedAt(null);

        NoReplyMailbox saved = repository.save(mailbox);

        String action = exists ? AppConstants.ACTION_UPDATE : AppConstants.ACTION_CREATE;
        logAudit(SINGLETON_ID, action, username, "Configured No-Reply Mailbox address in Azure: " + saved.getEmailAddress());

        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteMailbox(String username) {
        NoReplyMailbox mailbox = repository.findByIdAndDeletedAtIsNull(SINGLETON_ID)
                .orElseThrow(() -> new IllegalArgumentException("No-Reply Mailbox configuration not found."));

        // Disable Azure Cloud integration
        azureMailboxConfigService.disableAzureMailbox(mailbox.getEmailAddress());

        mailbox.setDeletedBy(username);
        mailbox.setDeletedAt(ZonedDateTime.now());
        repository.save(mailbox);

        logAudit(SINGLETON_ID, AppConstants.ACTION_DELETE, username, "Soft deleted No-Reply Mailbox configuration ID: " + SINGLETON_ID);
    }

    private void logAudit(Long id, String action, String username, String details) {
		/*
		 * auditLogRepository.save(AuditLog.builder() .entityName("NoReplyMailbox")
		 * .entityId(id) .actionType(action) .performedBy(username)
		 * .timestamp(ZonedDateTime.now()) .details(details) .build());
		 */
    }
}