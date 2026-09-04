package gov.fdic.tip.emailmanager.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class DistributionListSummaryDto {
    private Long id;
    private String name;
    private Long memberCount;
    private String memberDisplayLabel; // Formatted output, e.g., "10 Contacts"
    private String status;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;
    private boolean canEdit;
    private boolean canDelete;
}