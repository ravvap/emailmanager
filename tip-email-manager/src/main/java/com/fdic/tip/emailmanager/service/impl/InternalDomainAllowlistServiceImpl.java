package com.fdic.tip.emailmanager.service.impl;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fdic.tip.emailmanager.constant.AppConstants;
import com.fdic.tip.emailmanager.dto.DomainAllowlistDto;
import com.fdic.tip.emailmanager.entity.AuditLog;
import com.fdic.tip.emailmanager.entity.InternalDomainAllowlist;
import com.fdic.tip.emailmanager.mapper.InternalDomainAllowlistMapper;
import com.fdic.tip.emailmanager.repository.InternalDomainAllowlistRepository;
import com.fdic.tip.emailmanager.service.InternalDomainAllowlistService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalDomainAllowlistServiceImpl implements InternalDomainAllowlistService {

    private final InternalDomainAllowlistRepository repository;
     private final InternalDomainAllowlistMapper mapper;

    @Override
    @Transactional
    public DomainAllowlistDto createDomain(DomainAllowlistDto dto, String username) {
        if (repository.existsByDomainAndDeletedAtIsNull(dto.getDomain())) {
            throw new IllegalArgumentException("Domain already exists in allowlist: " + dto.getDomain());
        }

        InternalDomainAllowlist entity = mapper.toEntity(dto);
        entity.setCreatedBy(username);
        entity.setCreatedAt(ZonedDateTime.now());

        InternalDomainAllowlist saved = repository.save(entity);
        logAudit(saved.getId(), AppConstants.ACTION_CREATE, username, "Added domain: " + saved.getDomain());

        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public DomainAllowlistDto updateDomain(Long id, DomainAllowlistDto dto, String username) {
        InternalDomainAllowlist existing = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Domain entry not found with ID: " + id));

        if (repository.existsByDomainAndIdNotAndDeletedAtIsNull(dto.getDomain(), id)) {
            throw new IllegalArgumentException("Domain already exists in allowlist: " + dto.getDomain());
        }

        boolean statusChanged = !existing.getStatus().equalsIgnoreCase(dto.getStatus());

        mapper.updateEntityFromDto(dto, existing);
        existing.setUpdatedBy(username);
        existing.setUpdatedAt(ZonedDateTime.now());

        InternalDomainAllowlist updated = repository.save(existing);

        String action = statusChanged ? AppConstants.ACTION_STATUS_CHANGE : AppConstants.ACTION_UPDATE;
        logAudit(updated.getId(), action, username, "Updated domain: " + updated.getDomain());

        return mapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteDomain(Long id, String username) {
        InternalDomainAllowlist domain = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Domain entry not found with ID: " + id));

        if (!AppConstants.STATUS_INACTIVE.equalsIgnoreCase(domain.getStatus())) {
            throw new IllegalStateException("Cannot delete an active domain. Please deactivate it first.");
        }

        domain.setDeletedBy(username);
        domain.setDeletedAt(ZonedDateTime.now());
        repository.save(domain);

        logAudit(id, AppConstants.ACTION_DELETE, username, "Soft deleted domain ID: " + id);
    }

    @Override
    public List<DomainAllowlistDto> getAllDomains() {
        return repository.findByDeletedAtIsNull().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public DomainAllowlistDto getDomainById(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Domain entry not found with ID: " + id));
    }

    private void logAudit(Long id, String action, String username, String details) {
		/*
		 * auditLogRepository.save(AuditLog.builder()
		 * .entityName("InternalDomainAllowlist") .entityId(id) .actionType(action)
		 * .performedBy(username) .timestamp(ZonedDateTime.now()) .details(details)
		 * .build());
		 */
    }
}