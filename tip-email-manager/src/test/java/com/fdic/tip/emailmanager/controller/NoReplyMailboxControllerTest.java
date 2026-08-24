package com.fdic.tip.emailmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fdic.tip.emailmanager.dto.NoReplyMailboxDto;
import com.fdic.tip.emailmanager.service.NoReplyMailboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoReplyMailboxController.class)
@AutoConfigureMockMvc(addFilters = false)
class NoReplyMailboxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NoReplyMailboxService service;

    private NoReplyMailboxDto dto;

    @BeforeEach
    void setUp() {
        dto = new NoReplyMailboxDto();
        dto.setId(1L);
        dto.setEmailAddress("noreply@fdic.gov");
    }

    @Test
    void getMailbox_Returns200Ok() throws Exception {
        when(service.getMailbox()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/no-reply-mailbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.emailAddress").value("noreply@fdic.gov"));
    }

    @Test
    void saveOrUpdateMailbox_ValidPayload_Returns200Ok() throws Exception {
        when(service.saveOrUpdateMailbox(any(NoReplyMailboxDto.class), anyString())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/no-reply-mailbox")
                        .header("X-User-Name", "adminUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.emailAddress").value("noreply@fdic.gov"));
    }

    @Test
    void saveOrUpdateMailbox_InvalidEmail_Returns400BadRequest() throws Exception {
        dto.setEmailAddress("invalid-email-address");

        mockMvc.perform(put("/api/v1/no-reply-mailbox")
                        .header("X-User-Name", "adminUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveOrUpdateMailbox_BlankEmail_Returns400BadRequest() throws Exception {
        dto.setEmailAddress("");

        mockMvc.perform(put("/api/v1/no-reply-mailbox")
                        .header("X-User-Name", "adminUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteMailbox_Returns204NoContent() throws Exception {
        doNothing().when(service).deleteMailbox(anyString());

        mockMvc.perform(delete("/api/v1/no-reply-mailbox")
                        .header("X-User-Name", "adminUser"))
                .andExpect(status().isNoContent());
    }
}