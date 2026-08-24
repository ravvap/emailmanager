package com.fdic.tip.emailmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fdic.tip.emailmanager.dto.DomainAllowlistDto;
import com.fdic.tip.emailmanager.service.InternalDomainAllowlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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

@WebMvcTest(InternalDomainAllowlistController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalDomainAllowlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InternalDomainAllowlistService service;

    private DomainAllowlistDto dto;

    @BeforeEach
    void setUp() {
        dto = new DomainAllowlistDto();
        dto.setId(1L);
        dto.setDomain("fdic.gov");
        dto.setStatus("ACTIVE");
    }

    @Test
    void createDomain_ValidPayload_Returns201Created() throws Exception {
        when(service.createDomain(any(DomainAllowlistDto.class), anyString())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/internal-domains")
                        .header("X-User-Name", "testuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.domain").value("fdic.gov"));
    }

    @Test
    void createDomain_InvalidDomainFormat_Returns400BadRequest() throws Exception {
        DomainAllowlistDto invalidDto = new DomainAllowlistDto();
        invalidDto.setDomain("not-a-valid-domain"); // Fails @Pattern validation
        invalidDto.setStatus("ACTIVE");

        mockMvc.perform(post("/api/v1/internal-domains")
                        .header("X-User-Name", "testuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateDomain_ValidPayload_Returns200Ok() throws Exception {
        when(service.updateDomain(eq(1L), any(DomainAllowlistDto.class), anyString())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/internal-domains/1")
                        .header("X-User-Name", "testuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domain").value("fdic.gov"));
    }

    @Test
    void deleteDomain_Returns204NoContent() throws Exception {
        doNothing().when(service).deleteDomain(eq(1L), anyString());

        mockMvc.perform(delete("/api/v1/internal-domains/1")
                        .header("X-User-Name", "testuser"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAllDomains_ReturnsList() throws Exception {
        when(service.getAllDomains()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/internal-domains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].domain").value("fdic.gov"));
    }
}