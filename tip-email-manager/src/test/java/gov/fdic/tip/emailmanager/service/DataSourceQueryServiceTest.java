package gov.fdic.tip.emailmanager.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.fdic.tip.emailmanager.dto.CreateQueryRequest;
import gov.fdic.tip.emailmanager.dto.DataSourceQueryResponse;
import gov.fdic.tip.emailmanager.entity.DataSourceQuery;
import gov.fdic.tip.emailmanager.entity.QueryStatus;
import gov.fdic.tip.emailmanager.repository.DataSourceQueryRepository;

@ExtendWith(MockitoExtension.class)
class DataSourceQueryServiceTest {

    @Mock
    private DataSourceQueryRepository repository;

    @InjectMocks
    private DataSourceQueryService service;

    private DataSourceQuery sampleQuery;
    private UUID queryId;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        queryId = UUID.randomUUID();
        assetId = UUID.randomUUID();

        sampleQuery = new DataSourceQuery();
        sampleQuery.setId(queryId);
        sampleQuery.setAssetId(assetId);
        sampleQuery.setName("Test Query");
        sampleQuery.setConnectionId(UUID.randomUUID());
        sampleQuery.setSqlText("SELECT id, name FROM users");
        sampleQuery.setVersion(1);
        sampleQuery.setStatus(QueryStatus.PENDING_REVIEW);
        sampleQuery.setReferenceCount(0);
        sampleQuery.setCreatedBy("author_user");
        sampleQuery.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    void testAddQuery_Success() {
        CreateQueryRequest request = new CreateQueryRequest(
                "New Query", UUID.randomUUID(), "SELECT col1 FROM table1", null
        );

        when(repository.save(any(DataSourceQuery.class))).thenAnswer(invocation -> {
            DataSourceQuery q = invocation.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
        });

        DataSourceQueryResponse response = service.addQuery(request, "author_user");

        assertNotNull(response);
        assertEquals("New Query", response.name());
        assertEquals(QueryStatus.PENDING_REVIEW, response.status());
        verify(repository, times(1)).save(any(DataSourceQuery.class));
    }

    @Test
    void testAddQuery_InvalidSql_ThrowsException() {
        CreateQueryRequest request = new CreateQueryRequest(
                "Bad Query", UUID.randomUUID(), "DROP TABLE users;", null
        );

        assertThrows(IllegalArgumentException.class, () -> service.addQuery(request, "author_user"));
        verify(repository, never()).save(any());
    }

    @Test
    void testEditQuery_Success() {
        sampleQuery.setStatus(QueryStatus.ACTIVE);
        when(repository.findById(queryId)).thenReturn(Optional.of(sampleQuery));
        when(repository.save(any(DataSourceQuery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateQueryRequest request = new CreateQueryRequest(
                "Updated Query", sampleQuery.getConnectionId(), "SELECT col1, col2 FROM table1", null
        );

        DataSourceQueryResponse response = service.editQuery(queryId, request, "editor_user");

        assertNotNull(response);
        assertEquals(2, response.version());
        assertEquals(QueryStatus.PENDING_REVIEW, response.status());
    }

    @Test
    void testEditQuery_NotActive_ThrowsException() {
        sampleQuery.setStatus(QueryStatus.PENDING_REVIEW);
        when(repository.findById(queryId)).thenReturn(Optional.of(sampleQuery));

        CreateQueryRequest request = new CreateQueryRequest(
                "Updated Query", sampleQuery.getConnectionId(), "SELECT col1 FROM table1", null
        );

        assertThrows(IllegalStateException.class, () -> service.editQuery(queryId, request, "editor_user"));
    }

    @Test
    void testApproveQuery_Success() {
        when(repository.findById(queryId)).thenReturn(Optional.of(sampleQuery));
        when(repository.save(any(DataSourceQuery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DataSourceQueryResponse response = service.approveQuery(queryId, "reviewer_user");

        assertNotNull(response);
        assertEquals(QueryStatus.ACTIVE, response.status());
        assertEquals("reviewer_user", response.reviewedBy());
    }

    @Test
    void testApproveQuery_MakerCheckerViolation_ThrowsException() {
        when(repository.findById(queryId)).thenReturn(Optional.of(sampleQuery)); // createdBy is "author_user"

        assertThrows(SecurityException.class, () -> service.approveQuery(queryId, "author_user"));
    }

    @Test
    void testRejectQuery_Success() {
        when(repository.findById(queryId)).thenReturn(Optional.of(sampleQuery));
        when(repository.save(any(DataSourceQuery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DataSourceQueryResponse response = service.rejectQuery(queryId, "reviewer_user");

        assertNotNull(response);
        assertEquals(QueryStatus.REJECTED, response.status());
    }

    @Test
    void testRetireQuery_Success() {
        when(repository.findById(queryId)).thenReturn(Optional.of(sampleQuery));
        when(repository.save(any(DataSourceQuery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DataSourceQueryResponse response = service.retireQuery(queryId);

        assertNotNull(response);
        assertEquals(QueryStatus.RETIRED, response.status());
    }

    @Test
    void testDeleteQuery_Success() {
        sampleQuery.setReferenceCount(0);
        when(repository.findById(queryId)).thenReturn(Optional.of(sampleQuery));
        doNothing().when(repository).delete(sampleQuery);

        assertDoesNotThrow(() -> service.deleteQuery(queryId));
        verify(repository, times(1)).delete(sampleQuery);
    }

    @Test
    void testDeleteQuery_Referenced_ThrowsException() {
        sampleQuery.setReferenceCount(2);
        when(repository.findById(queryId)).thenReturn(Optional.of(sampleQuery));

        assertThrows(IllegalStateException.class, () -> service.deleteQuery(queryId));
        verify(repository, never()).delete(any());
    }

    @Test
    void testListAllQueries() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sampleQuery));

        List<DataSourceQueryResponse> result = service.listAllQueries();

        assertEquals(1, result.size());
        assertEquals(sampleQuery.getName(), result.get(0).name());
    }

    @Test
    void testGetAssetVersionHistory() {
        when(repository.findByAssetIdOrderByVersionDesc(assetId)).thenReturn(List.of(sampleQuery));

        List<DataSourceQueryResponse> result = service.getAssetVersionHistory(assetId);

        assertEquals(1, result.size());
    }

    @Test
    void testExtractColumnsFromSql_Success() {
        String sql = "SELECT col1 AS alias1, t.col2 FROM my_table t";
        List<String> columns = service.extractColumnsFromSql(sql);

        assertEquals(2, columns.size());
        assertTrue(columns.contains("alias1"));
        assertTrue(columns.contains("col2"));
    }

    @Test
    void testExtractColumnsFromSql_InvalidSql_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.extractColumnsFromSql("INSERT INTO table VALUES (1)"));
    }
}