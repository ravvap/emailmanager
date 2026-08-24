package com.fdic.tip.emailmanager.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fdic.tip.emailmanager.dto.NoReplyMailboxDto;
import com.fdic.tip.emailmanager.entity.NoReplyMailbox;
import com.fdic.tip.emailmanager.mapper.NoReplyMailboxMapper;
import com.fdic.tip.emailmanager.repository.NoReplyMailboxRepository;
import com.fdic.tip.emailmanager.service.AzureMailboxConfigService;

@ExtendWith(MockitoExtension.class)
class NoReplyMailboxServiceImplTest {

    @Mock
    private NoReplyMailboxRepository repository;

    
    @Mock
    private NoReplyMailboxMapper mapper;

    @Mock
    private AzureMailboxConfigService azureMailboxConfigService;

    @InjectMocks
    private NoReplyMailboxServiceImpl service;

    private NoReplyMailboxDto dto;
    private NoReplyMailbox entity;

    @BeforeEach
    void setUp() {
        dto = new NoReplyMailboxDto();
        dto.setId(1L);
        dto.setEmailAddress("noreply@fdic.gov");

        entity = new NoReplyMailbox();
        entity.setId(1L);
        entity.setEmailAddress("noreply@fdic.gov");
    }

    @Test
    void saveOrUpdateMailbox_Success() {
        when(repository.existsByEmailAddressAndIdNotAndDeletedAtIsNull("noreply@fdic.gov", 1L)).thenReturn(false);
        doNothing().when(azureMailboxConfigService).configureAzureMailbox("noreply@fdic.gov");
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(NoReplyMailbox.class))).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto);

        NoReplyMailboxDto result = service.saveOrUpdateMailbox(dto, "adminUser");

        assertNotNull(result);
        assertEquals("noreply@fdic.gov", result.getEmailAddress());
        verify(azureMailboxConfigService).configureAzureMailbox("noreply@fdic.gov");
     }

    @Test
    void deleteMailbox_Success() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(entity));
        doNothing().when(azureMailboxConfigService).disableAzureMailbox("noreply@fdic.gov");

        service.deleteMailbox("adminUser");

        verify(azureMailboxConfigService).disableAzureMailbox("noreply@fdic.gov");
        verify(repository).save(entity);
     }

    @Test
    void getMailbox_NotFound_ThrowsException() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getMailbox());
    }
}