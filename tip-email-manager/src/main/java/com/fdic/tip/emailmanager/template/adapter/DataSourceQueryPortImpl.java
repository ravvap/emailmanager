package com.fdic.tip.emailmanager.template.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fdic.tip.emailmanager.common.constants.DataSourceQueryConstants;
import com.fdic.tip.emailmanager.template.service.DataSourceQueryPort;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JDBC adapter into the EM-19 Data Source Query catalog.
 *
 * EM-19 is owned and versioned separately from this module (see EM-8
 * AC: "the template references a specific approved version of that
 * query"), and its actual DDL wasn't available when this was written.
 * The table/column names and the shape of "authorization" below are
 * this adapter's best assumption, documented in
 * DataSourceQueryConstants — swap this class out (or adjust the SQL)
 * once EM-19's real schema is confirmed. If EM-19 turns out to be a
 * separate deployable rather than a shared database/schema, this
 * becomes a REST/Feign client instead of a JdbcTemplate adapter, but
 * the DataSourceQueryPort contract it fulfills doesn't change either
 * way — that's the point of keeping it behind a port.
 */
@Component
public class DataSourceQueryPortImpl implements DataSourceQueryPort {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DataSourceQueryPortImpl(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isAuthorized(String userId, Long dataSourceQueryId) {
        Boolean requiresGrant;
        try {
            requiresGrant = jdbcTemplate.queryForObject(
                    "SELECT requires_permission_grant FROM " + DataSourceQueryConstants.TABLE_QUERY
                            + " WHERE query_id = ? AND deleted_at IS NULL",
                    Boolean.class, dataSourceQueryId);
        } catch (EmptyResultDataAccessException ex) {
            throw new EntityNotFoundException(DataSourceQueryConstants.MSG_QUERY_NOT_FOUND);
        }

        if (Boolean.FALSE.equals(requiresGrant)) {
            return true;
        }

        Integer grantCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + DataSourceQueryConstants.TABLE_QUERY_PERMISSION
                        + " WHERE query_id = ? AND principal_type = ? AND principal_id = ?",
                Integer.class, dataSourceQueryId, DataSourceQueryConstants.PRINCIPAL_TYPE_USER, userId);

        return grantCount != null && grantCount > 0;
    }

    @Override
    public int getApprovedVersion(Long dataSourceQueryId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT version_number FROM " + DataSourceQueryConstants.TABLE_QUERY_VERSION
                            + " WHERE query_id = ? AND status = ? AND deleted_at IS NULL "
                            + "ORDER BY version_number DESC LIMIT 1",
                    Integer.class, dataSourceQueryId, DataSourceQueryConstants.STATUS_APPROVED);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException(DataSourceQueryConstants.MSG_NO_APPROVED_VERSION);
        }
    }

    @Override
    public List<String> getColumns(Long dataSourceQueryId, int queryVersion) {
        String columnsJson;
        try {
            // Assumes each approved query version snapshots its result
            // columns as a JSON array at approval time (e.g. ["full_name",
            // "email", "due_date"]) — see EM-8 AC: merge fields are picked
            // from "the query's actual returned columns", not typed by hand.
            columnsJson = jdbcTemplate.queryForObject(
                    "SELECT result_columns FROM " + DataSourceQueryConstants.TABLE_QUERY_VERSION
                            + " WHERE query_id = ? AND version_number = ? AND deleted_at IS NULL",
                    String.class, dataSourceQueryId, queryVersion);
        } catch (EmptyResultDataAccessException ex) {
            throw new EntityNotFoundException(DataSourceQueryConstants.MSG_VERSION_NOT_FOUND);
        }

        if (columnsJson == null || columnsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(columnsJson, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Could not parse stored column list for query " + dataSourceQueryId, ex);
        }
    }
}
