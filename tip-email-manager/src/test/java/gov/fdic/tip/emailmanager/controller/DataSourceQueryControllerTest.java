package gov.fdic.tip.emailmanager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.fdic.tip.emailmanager.dto.CreateQueryRequest;
import gov.fdic.tip.emailmanager.dto.DataSourceQueryResponse;
import gov.fdic.tip.emailmanager.entity.QueryStatus;
import gov.fdic.tip.emailmanager.service.DataSourceQueryService;

@WebMvcTest(DataSourceQueryController.class)
class DataSourceQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataSourceQueryService service;

    private final UUID queryId = UUID.randomUUID();
    private final UUID assetId = UUID.randomUUID();

    private DataSourceQueryResponse sampleResponse() {
        return new DataSourceQueryResponse(
                queryId, assetId, "Test Query", UUID.randomUUID(),
                "SELECT * FROM table", null, 1, QueryStatus.PENDING_REVIEW,
                0, "user1", OffsetDateTime.now(), null, null
        );
    }

    @Test
    void testListAllQueries() throws Exception {
        when(service.listAllQueries()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/data-source-queries")
                        .with(user("admin").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Query"));
    }

    @Test
    void testGetAssetHistory() throws Exception {
        when(service.getAssetVersionHistory(assetId)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/data-source-queries/history/" + assetId)
                        .with(user("admin").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assetId").value(assetId.toString()));
    }

    @Test
    void testAddQuery() throws Exception {
        CreateQueryRequest request = new CreateQueryRequest(
                "New Query", UUID.randomUUID(), "SELECT id FROM table", null
        );
        when(service.addQuery(any(CreateQueryRequest.class), eq("admin"))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/data-source-queries")
                        .with(user("admin").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Query"));
    }

    @Test
    void testEditQuery() throws Exception {
        CreateQueryRequest request = new CreateQueryRequest(
                "Updated Query", UUID.randomUUID(), "SELECT id FROM table", null
        );
        when(service.editQuery(eq(queryId), any(CreateQueryRequest.class), eq("admin"))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/data-source-queries/" + queryId + "/edit")
                        .with(user("admin").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testApproveQuery() throws Exception {
        when(service.approveQuery(eq(queryId), eq("admin"))).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/data-source-queries/" + queryId + "/approve")
                        .with(user("admin").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void testRejectQuery() throws Exception {
        when(service.rejectQuery(eq(queryId), eq("admin"))).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/data-source-queries/" + queryId + "/reject")
                        .with(user("admin").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void testRetireQuery() throws Exception {
        when(service.retireQuery(queryId)).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/data-source-queries/" + queryId + "/retire")
                        .with(user("admin").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteQuery() throws Exception {
        doNothing().when(service).deleteQuery(queryId);

        mockMvc.perform(delete("/api/v1/data-source-queries/" + queryId)
                        .with(user("admin").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void testGetQueryColumns() throws Exception {
        when(service.getQueryColumns(queryId)).thenReturn(List.of("col1", "col2"));

        mockMvc.perform(get("/api/v1/data-source-queries/" + queryId + "/columns")
                        .with(user("admin").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("col1"));
    }
}