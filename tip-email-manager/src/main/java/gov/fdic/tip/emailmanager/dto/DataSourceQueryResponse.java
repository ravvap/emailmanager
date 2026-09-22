package gov.fdic.tip.emailmanager.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import gov.fdic.tip.emailmanager.entity.DataSourceQuery;
import gov.fdic.tip.emailmanager.entity.QueryStatus;

/**
 * Data transfer object for returning Data Source Query version details.
 *
 * @author prasad ravva
 */
public record DataSourceQueryResponse(
    UUID id,
    UUID assetId,
    String name,
    String connectionId,
    String sqlText,
    String parameters,
    Integer version,
    QueryStatus status,
    Integer referenceCount,
    String createdBy,
    OffsetDateTime createdAt,
    String reviewedBy,
    OffsetDateTime reviewedAt
) {
    public static DataSourceQueryResponse fromEntity(DataSourceQuery entity) {
        return new DataSourceQueryResponse(
            entity.getId(),
            entity.getAssetId(),
            entity.getName(),
            entity.getConnectionId(),
            entity.getSqlText(),
            entity.getParameters(),
            entity.getVersion(),
            entity.getStatus(),
            entity.getReferenceCount(),
            entity.getCreatedBy(),
            entity.getCreatedAt(),
            entity.getReviewedBy(),
            entity.getReviewedAt()
        );
    }
}
