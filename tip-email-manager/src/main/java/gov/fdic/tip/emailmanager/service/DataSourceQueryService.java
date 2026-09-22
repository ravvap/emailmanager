package gov.fdic.tip.emailmanager.service;

import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fdic.tip.emailmanager.constant.AppConstants;

import gov.fdic.tip.emailmanager.dto.CreateQueryRequest;
import gov.fdic.tip.emailmanager.dto.DataSourceQueryResponse;
import gov.fdic.tip.emailmanager.entity.DataSourceQuery;
import gov.fdic.tip.emailmanager.entity.QueryStatus;
import gov.fdic.tip.emailmanager.repository.DataSourceQueryRepository;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
 

/**
 * Service orchestrating governance workflows and strict validations for Data Source Queries.
 *
 * @author prasad ravva
 */
@Service
public class DataSourceQueryService {

    private final DataSourceQueryRepository repository;

    public DataSourceQueryService(DataSourceQueryRepository repository) {
        this.repository = repository;
    }

    /**
     * Submits a new Data Source Query (v1) in PENDING_REVIEW state.
     */
    @Transactional
    public DataSourceQueryResponse addQuery(CreateQueryRequest request, String currentUsername) {
        validateReadOnlySql(request.sqlText());

        DataSourceQuery query = new DataSourceQuery();
        query.setAssetId(UUID.randomUUID());
        query.setName(request.name());
        query.setConnectionId(request.connectionId());
        query.setSqlText(request.sqlText());
        query.setParameters(request.parameters());
        query.setVersion(1);
        query.setStatus(QueryStatus.PENDING_REVIEW);
        query.setReferenceCount(0);
        query.setCreatedBy(currentUsername);
        query.setCreatedAt(OffsetDateTime.now());

        return DataSourceQueryResponse.fromEntity(repository.save(query));
    }

    /**
     * Creates a new version (vN+1) in PENDING_REVIEW status.
     * VALIDATION: Edit is available ONLY on ACTIVE versions.
     */
    @Transactional
    public DataSourceQueryResponse editQuery(UUID currentVersionId, CreateQueryRequest request, String currentUsername) {
        validateReadOnlySql(request.sqlText());

        DataSourceQuery currentVersion = repository.findById(currentVersionId)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.MSG_QUERY_NOT_FOUND));

        // VALIDATION: Edit available ONLY for ACTIVE versions
        if (currentVersion.getStatus() != QueryStatus.ACTIVE) {
            throw new IllegalStateException("Edit is only available for ACTIVE query versions.");
        }

        DataSourceQuery newVersion = new DataSourceQuery();
        newVersion.setAssetId(currentVersion.getAssetId());
        newVersion.setName(request.name());
        newVersion.setConnectionId(request.connectionId());
        newVersion.setSqlText(request.sqlText());
        newVersion.setParameters(request.parameters());
        newVersion.setVersion(currentVersion.getVersion() + 1);
        newVersion.setStatus(QueryStatus.PENDING_REVIEW);
        newVersion.setReferenceCount(0);
        newVersion.setCreatedBy(currentUsername);
        newVersion.setCreatedAt(OffsetDateTime.now());

        return DataSourceQueryResponse.fromEntity(repository.save(newVersion));
    }

    /**
     * Approves a pending query version.
     * VALIDATIONS: Must be in PENDING_REVIEW state; Submitter CANNOT be the approver.
     */
    @Transactional
    public DataSourceQueryResponse approveQuery(UUID id, String reviewerUsername) {
        DataSourceQuery query = validateForReview(id, reviewerUsername);
        query.setStatus(QueryStatus.ACTIVE);
        query.setReviewedBy(reviewerUsername);
        query.setReviewedAt(OffsetDateTime.now());

        return DataSourceQueryResponse.fromEntity(repository.save(query));
    }

    /**
     * Rejects a pending query version.
     * VALIDATIONS: Must be in PENDING_REVIEW state; Submitter CANNOT be the reviewer.
     */
    @Transactional
    public DataSourceQueryResponse rejectQuery(UUID id, String reviewerUsername) {
        DataSourceQuery query = validateForReview(id, reviewerUsername);
        query.setStatus(QueryStatus.REJECTED);
        query.setReviewedBy(reviewerUsername);
        query.setReviewedAt(OffsetDateTime.now());

        return DataSourceQueryResponse.fromEntity(repository.save(query));
    }

    /**
     * Retires an active query version (Soft Delete).
     */
    @Transactional
    public DataSourceQueryResponse retireQuery(UUID id) {
        DataSourceQuery query = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.MSG_QUERY_NOT_FOUND));

        query.setStatus(QueryStatus.RETIRED);
        return DataSourceQueryResponse.fromEntity(repository.save(query));
    }

    /**
     * Permanently deletes a query version.
     * VALIDATION: Delete is available ONLY for versions that are NOT referenced by any template.
     */
    @Transactional
    public void deleteQuery(UUID id) {
        DataSourceQuery query = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.MSG_QUERY_NOT_FOUND));

        // VALIDATION: Cannot delete if referenced by templates (referenceCount > 0)
        if (query.getReferenceCount() != null && query.getReferenceCount() > 0) {
            throw new IllegalStateException("Delete is only available for query versions that are not referenced by templates.");
        }

        repository.delete(query);
    }

    /**
     * Helper method validating Maker-Checker governance rules for Approve/Reject actions.
     */
    private DataSourceQuery validateForReview(UUID id, String reviewerUsername) {
        DataSourceQuery query = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(AppConstants.MSG_QUERY_NOT_FOUND));

        // VALIDATION: Approve/Reject available ONLY for PENDING_REVIEW versions
        if (query.getStatus() != QueryStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Approve/Reject actions are only allowed for query versions in 'PENDING_REVIEW' state.");
        }

        // VALIDATION: Submitter cannot be approver/reviewer
        if (query.getCreatedBy() != null && query.getCreatedBy().equalsIgnoreCase(reviewerUsername)) {
            throw new SecurityException("Maker-Checker Policy Violation: Submitter cannot approve or reject their own query submission.");
        }

        return query;
    }
    
    /**
     * Retrieves all query versions ordered by creation date descending.
     *
     * @return List of DataSourceQueryResponse objects.
     */
    @Transactional(readOnly = true)
    public List<DataSourceQueryResponse> listAllQueries() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(DataSourceQueryResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves full version history for a given query asset lineage.
     *
     * @param assetId The UUID of the query asset lineage.
     * @return List of DataSourceQueryResponse objects ordered by version descending.
     */
    @Transactional(readOnly = true)
    public List<DataSourceQueryResponse> getAssetVersionHistory(UUID assetId) {
        return repository.findByAssetIdOrderByVersionDesc(assetId).stream()
                .map(DataSourceQueryResponse::fromEntity)
                .toList();
    }

    /**
     * Robust SQL Validator ensuring ONLY single SELECT statements are allowed.
     * Rejects DML (INSERT, UPDATE, DELETE), DDL (DROP, ALTER, CREATE), and multi-statement queries.
     */
    private void validateReadOnlySql(String sqlText) {
        if (sqlText == null || sqlText.isBlank()) {
            throw new IllegalArgumentException("SQL text cannot be blank.");
        }

        try {
            // Parse SQL statement using JSqlParser
        	net.sf.jsqlparser.statement.Statement parsedStatement = CCJSqlParserUtil.parse(sqlText);

            // VALIDATION: Reject any non-SELECT query
            if (!(parsedStatement instanceof Select)) {
                throw new IllegalArgumentException("Invalid SQL operation: Only SELECT queries are permitted. Data modification (INSERT, UPDATE, DELETE) and DDL operations are strictly prohibited.");
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new IllegalArgumentException("Invalid SQL syntax or unauthorized query format: " + e.getMessage());
        }
    }
}