package com.fdic.tip.emailmanager.service.impl;

import com.fdic.tip.emailmanager.dto.DomainAllowlistDto;
import com.fdic.tip.emailmanager.entity.AuditLog;
import com.fdic.tip.emailmanager.entity.InternalDomainAllowlist;
import com.fdic.tip.emailmanager.mapper.InternalDomainAllowlistMapper;
import com.fdic.tip.emailmanager.repository.InternalDomainAllowlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalDomainAllowlistServiceImplTest {

    @Mock
    private InternalDomainAllowlistRepository repository;
 
    @Mock
    private InternalDomainAllowlistMapper mapper;

    @InjectMocks
    private InternalDomainAllowlistServiceImpl service;

    private DomainAllowlistDto dto;
    private InternalDomainAllowlist entity;

    @BeforeEach
    void setUp() {
        dto = new DomainAllowlistDto();
        dto.setId(1L);
        dto.setDomain("fdic.gov");
        dto.setStatus("ACTIVE");

        entity = new InternalDomainAllowlist();
        entity.setId(1L);
        entity.setDomain("fdic.gov");
        entity.setStatus("ACTIVE");
    }

    @Test
    void createDomain_Success() {
        when(repository.existsByDomainAndDeletedAtIsNull("fdic.gov")).thenReturn(false);
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(any(InternalDomainAllowlist.class))).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto);

        DomainAllowlistDto result = service.createDomain(dto, "testuser");

        assertNotNull(result);
        assertEquals("fdic.gov", result.getDomain());
     }

    @Test
    void createDomain_DuplicateDomain_ThrowsException() {
        when(repository.existsByDomainAndDeletedAtIsNull("fdic.gov")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.createDomain(dto, "testuser"));
        verify(repository, never()).save(any(InternalDomainAllowlist.class));
    }

    @Test
    void deleteDomain_ActiveStatus_ThrowsException() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(entity));

        assertThrows(IllegalStateException.class, () -> service.deleteDomain(1L, "testuser"));
    }

    @Test
    void deleteDomain_InactiveStatus_Success() {
        entity.setStatus("INACTIVE");
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(entity));

        service.deleteDomain(1L, "testuser");

        verify(repository).save(entity);
     }

    @Test
    void getAllDomains_Success() {
        when(repository.findByDeletedAtIsNull()).thenReturn(List.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        List<DomainAllowlistDto> results = service.getAllDomains();

        assertEquals(1, results.size());
        assertEquals("fdic.gov", results.get(0).getDomain());
    }
}