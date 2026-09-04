package gov.fdic.tip.emailmanager.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;

@Data
public class DistributionListViewDto {
    private Long id;
    private String name;
    private String status;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;
    
    // Paginated list of members supporting Name/Email filtering
    private Page<DistributionListMemberDto> members;
}