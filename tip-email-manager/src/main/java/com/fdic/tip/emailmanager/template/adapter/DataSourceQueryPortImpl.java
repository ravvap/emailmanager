package com.fdic.tip.emailmanager.template.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fdic.tip.emailmanager.common.constants.DataSourceQueryConstants;
import lombok.extern.slf4j.Slf4j;
import com.fdic.tip.emailmanager.template.service.DataSourceQueryPort;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Adapter over the real {@code data_source_query} table (EM-1/EM-19
 * schema, plain public schema, no separate version or permission
 * table): {@code asset_id} groups a query's versions, each version is
 * its own row with its own UUID row id and a status of
 * PENDING_REVIEW/ACTIVE/REJECTED/RETIRED.
 *
 * <p>"Approved" = status ACTIVE (there's no literal APPROVED value).
 * Row lookups (approved version, authorization baseline) are a local
 * JdbcTemplate query since data_source_query lives in the same
 * database as the rest of Email Manager. Result columns are NOT stored
 * on the row, so those come from the platform's existing "get data
 * source query columns" endpoint instead.
 *
 * <p><b>Authorization gap:</b> this schema has no permission/ACL table,
 * so {@link #isAuthorized} can only confirm the (asset_id, version) row
 * exists and is ACTIVE — it does not enforce the "access to sensitive
 * sources is permission-controlled" EM-8 AC. If per-user/per-role
 * grants live in a separate catalog (e.g. under Data Manager), this
 * method needs a real authorization check wired in once that's
 * confirmed; as written it under-enforces rather than silently
 * over-enforcing.
 */
@Slf4j
@Component
public class DataSourceQueryPortImpl implements DataSourceQueryPort {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final String columnsEndpointBaseUrl;

    public DataSourceQueryPortImpl(
            JdbcTemplate jdbcTemplate,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${" + DataSourceQueryConstants.PROP_COLUMNS_ENDPOINT_BASE_URL + "}") String columnsEndpointBaseUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplateBuilder.build();
        this.columnsEndpointBaseUrl = columnsEndpointBaseUrl;
    }

    @Override
    public boolean isAuthorized(String userId, UUID dataSourceQueryId) {
        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + DataSourceQueryConstants.TABLE_QUERY
                        + " WHERE asset_id = ? AND status = ?",
                Integer.class, dataSourceQueryId, DataSourceQueryConstants.STATUS_ACTIVE);
        // See class javadoc: this confirms the query is currently usable,
        // not that this specific user/role has been granted access to it.
        return activeCount != null && activeCount > 0;
    }

    @Override
    public int getApprovedVersion(UUID dataSourceQueryId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT version FROM " + DataSourceQueryConstants.TABLE_QUERY
                            + " WHERE asset_id = ? AND status = ? ORDER BY version DESC LIMIT 1",
                    Integer.class, dataSourceQueryId, DataSourceQueryConstants.STATUS_ACTIVE);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException(DataSourceQueryConstants.MSG_NO_APPROVED_VERSION);
        }
    }

    @Override
    public List<String> getColumns(UUID dataSourceQueryId, int queryVersion) {
        UUID rowId = resolveRowId(dataSourceQueryId, queryVersion);
        try {
            JsonNode body = restTemplate.getForObject(columnsEndpointBaseUrl + "/{id}/columns", JsonNode.class, rowId);
            List<String> columns = extractColumnNames(body);
            if (columns.isEmpty()) {
                log.warn("Columns endpoint returned no column names for query {} v{} (row {}); raw response: {}",
                        dataSourceQueryId, queryVersion, rowId, body);
            }
            return columns;
        } catch (RestClientException ex) {
            log.error("Columns endpoint call failed for query {} v{} (row {}) at {}: {}",
                    dataSourceQueryId, queryVersion, rowId, columnsEndpointBaseUrl, ex.getMessage());
            throw new IllegalStateException(DataSourceQueryConstants.MSG_COLUMNS_FETCH_FAILED, ex);
        }
    }

    /**
     * The columns endpoint's exact response shape wasn't known when this was
     * written, so accept the common ones: ["a","b"], [{"name":"a"}, ...]
     * (name / columnName / column_name / column / label), or an object
     * wrapping either under columns / data / content / result.
     */
    private List<String> extractColumnNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        if (node == null || node.isNull()) {
            return names;
        }
        if (node.isArray()) {
            for (JsonNode element : node) {
                if (element.isTextual()) {
                    names.add(element.asText());
                } else if (element.isObject()) {
                    for (String key : new String[]{"name", "columnName", "column_name", "column", "label"}) {
                        if (element.hasNonNull(key)) {
                            names.add(element.get(key).asText());
                            break;
                        }
                    }
                }
            }
        } else if (node.isObject()) {
            for (String key : new String[]{"columns", "data", "content", "result"}) {
                if (node.has(key)) {
                    return extractColumnNames(node.get(key));
                }
            }
        }
        return names;
    }

    /** Resolves the (asset_id, version) pair we store to the row's own id, which the columns endpoint addresses by. */
    private UUID resolveRowId(UUID assetId, int version) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM " + DataSourceQueryConstants.TABLE_QUERY
                            + " WHERE asset_id = ? AND version = ?",
                    UUID.class, assetId, version);
        } catch (EmptyResultDataAccessException ex) {
            throw new EntityNotFoundException(DataSourceQueryConstants.MSG_VERSION_NOT_FOUND);
        }
    }
}
