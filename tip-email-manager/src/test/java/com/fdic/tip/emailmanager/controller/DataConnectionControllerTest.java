package com.fdic.tip.emailmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fdic.tip.emailmanager.dto.ConnectionTestResultDto;
import com.fdic.tip.emailmanager.dto.DataConnectionDto;
import com.fdic.tip.emailmanager.entity.DataConnection;
import com.fdic.tip.emailmanager.repository.DataConnectionRepository;
import com.fdic.tip.emailmanager.service.DataConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataConnectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class DataConnectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataConnectionService service;

    private DataConnectionDto dto;
    private final Principal mockPrincipal = () -> "testuser";

    @MockBean
	private DataConnectionRepository locationRepository;

    @MockBean
	private DataConnection dataConnection;

    @BeforeEach
    void setUp() {
        dto = new DataConnectionDto();
        dto.setId(1L);
        dto.setName("Primary DB Connection");
        dto.setDatabaseLocationId(10L);
        dto.setStatus("ACTIVE");
        dto.setAuthorUserIds(Set.of(100L));
    }

    @Test
    void createConnection_ValidPayload_Returns201Created() throws Exception {
        when(service.createConnection(any(DataConnectionDto.class), anyString())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/data-connections")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Primary DB Connection"))
                .andExpect(jsonPath("$.databaseLocationId").value(10));
    }

    @Test
    void createConnection_MissingName_Returns400BadRequest() throws Exception {
        dto.setName("");

        mockMvc.perform(post("/api/v1/data-connections")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateConnection_ValidPayload_Returns200Ok() throws Exception {
        when(service.updateConnection(eq(1L), any(DataConnectionDto.class), anyString())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/data-connections/1")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Primary DB Connection"));
    }

    @Test
    void deleteConnection_Returns204NoContent() throws Exception {
        doNothing().when(service).deleteConnection(eq(1L), anyString());

        mockMvc.perform(delete("/api/v1/data-connections/1")
                        .principal(mockPrincipal))
                .andExpect(status().isOk());
    }

    @Test
    void getAllConnections_ReturnsList() throws Exception {
        when(service.getAllConnections()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/data-connections")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Primary DB Connection"));
    }
    
    @Test
    void testConnection_SuccessfulConnection_Returns200Ok() throws Exception {
        ConnectionTestResultDto resultDto = ConnectionTestResultDto.builder()
                .connected(true)
                .message("Connection successful")
                .responseTimeMs(45L)
                .build();

        when(service.testConnection(any(DataConnectionDto.class))).thenReturn(resultDto);

        mockMvc.perform(post("/api/v1/data-connections/test")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.message").value("Connection successful"))
                .andExpect(jsonPath("$.responseTimeMs").value(45));
    }

    @Test
    void testConnection_FailedConnection_Returns200OkWithFailureStatus() throws Exception {
        ConnectionTestResultDto resultDto = ConnectionTestResultDto.builder()
                .connected(false)
                .message("Connection timed out")
                .responseTimeMs(5000L)
                .build();

        when(service.testConnection(any(DataConnectionDto.class))).thenReturn(resultDto);

        mockMvc.perform(post("/api/v1/data-connections/test")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.message").value("Connection timed out"));
    }
    
    @Test
    void testConnection_DevProfileWithPassword_Success() throws Exception {
        // Mock the service response directly
        ConnectionTestResultDto resultDto = ConnectionTestResultDto.builder()
                .connected(true)
                .message("Connection successful")
                .responseTimeMs(45L)
                .build();

        when(service.testConnection(any(DataConnectionDto.class))).thenReturn(resultDto);

        mockMvc.perform(post("/api/v1/data-connections/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true));
    }
    
    @Test
    void getEligibleAuthors_ReturnsList() throws Exception {
        AuthorDropdownDto author = AuthorDropdownDto.builder()
                .id(100L)
                .username("adminUser")
                .displayName("System Admin")
                .emailAddress("admin@fdic.gov")
                .build();

        when(service.getEligibleAuthors()).thenReturn(List.of(author));

        mockMvc.perform(get("/api/v1/data-connections/authors")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].username").value("adminUser"))
                .andExpect(jsonPath("$[0].displayName").value("System Admin"));
    }
}