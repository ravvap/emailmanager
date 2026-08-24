package com.fdic.tip.emailmanager.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fdic.tip.emailmanager.dto.DataConnectionDto;
import com.fdic.tip.emailmanager.entity.DataConnection;
import com.fdic.tip.emailmanager.entity.DatabaseLocation;
import com.fdic.tip.emailmanager.mapper.DataConnectionMapper;
import com.fdic.tip.emailmanager.repository.DataConnectionRepository;
import com.fdic.tip.emailmanager.repository.DatabaseLocationRepository;

@ExtendWith(MockitoExtension.class)
class DataConnectionServiceImplTest {

    @Mock
    private DataConnectionRepository repository;

    @Mock
    private DatabaseLocationRepository locationRepository;

 

    @Mock
    private DataConnectionMapper mapper;

    @InjectMocks
    private DataConnectionServiceImpl service;

    private DataConnectionDto dto;
    private DataConnection entity;
    private DatabaseLocation location;

    @BeforeEach
    void setUp() {
        location = new DatabaseLocation();
        location.setId(10L);
        location.setHostname("localhost");

        dto = new DataConnectionDto();
        dto.setId(1L);
        dto.setName("Primary DB Connection");
        dto.setDatabaseLocationId(10L);
        dto.setStatus("ACTIVE");
        dto.setAuthorUserIds(Set.of(100L));

        entity = new DataConnection();
        entity.setId(1L);
        entity.setName("Primary DB Connection");
        entity.setStatus("ACTIVE");
        entity.setDatabaseLocation(location);
        entity.setAuthorUserIds(Set.of(100L));
    }

    @Test
    void createConnection_Success() {
        when(repository.existsByNameAndDeletedAtIsNull("Primary DB Connection")).thenReturn(false);
        when(locationRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(location));
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(any(DataConnection.class))).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto);

        DataConnectionDto result = service.createConnection(dto, "testuser");

        assertNotNull(result);
        assertEquals("Primary DB Connection", result.getName());
     }

    @Test
    void createConnection_DuplicateName_ThrowsException() {
        when(repository.existsByNameAndDeletedAtIsNull("Primary DB Connection")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.createConnection(dto, "testuser"));
        verify(repository, never()).save(any(DataConnection.class));
    }

    @Test
    void createConnection_LocationNotFound_ThrowsException() {
        when(repository.existsByNameAndDeletedAtIsNull("Primary DB Connection")).thenReturn(false);
        when(locationRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.createConnection(dto, "testuser"));
        verify(repository, never()).save(any(DataConnection.class));
    }

    @Test
    void updateConnection_Success() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByNameAndIdNotAndDeletedAtIsNull("Primary DB Connection", 1L)).thenReturn(false);
        when(locationRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(location));
        when(repository.save(any(DataConnection.class))).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto);

        DataConnectionDto result = service.updateConnection(1L, dto, "testuser");

        assertNotNull(result);
        verify(mapper).updateEntityFromDto(dto, entity);
     }

    @Test
    void deleteConnection_UsedInTemplate_ThrowsException() {
        entity.setUsedInTemplate(true);
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(entity));

        assertThrows(IllegalStateException.class, () -> service.deleteConnection(1L, "testuser"));
        verify(repository, never()).save(any(DataConnection.class));
    }

    @Test
    void deleteConnection_ActiveStatus_ThrowsException() {
        entity.setUsedInTemplate(false);
        entity.setStatus("ACTIVE");
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(entity));

        assertThrows(IllegalStateException.class, () -> service.deleteConnection(1L, "testuser"));
        verify(repository, never()).save(any(DataConnection.class));
    }

    @Test
    void deleteConnection_InactiveStatus_Success() {
        entity.setUsedInTemplate(false);
        entity.setStatus("INACTIVE");
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(entity));

        service.deleteConnection(1L, "testuser");

        verify(repository).save(entity);
     }

    @Test
    void getAllConnections_Success() {
        when(repository.findByDeletedAtIsNull()).thenReturn(List.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        List<DataConnectionDto> results = service.getAllConnections();

        assertEquals(1, results.size());
        assertEquals("Primary DB Connection", results.get(0).getName());
    }

    @Test
    void getActiveConnectionsForAuthor_Success() {
        when(repository.findByStatusAndAuthorUserIdsContainingAndDeletedAtIsNull("ACTIVE", 100L))
                .thenReturn(List.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        List<DataConnectionDto> results = service.getActiveConnectionsForAuthor(100L);

        assertEquals(1, results.size());
        assertEquals("Primary DB Connection", results.get(0).getName());
    }
}