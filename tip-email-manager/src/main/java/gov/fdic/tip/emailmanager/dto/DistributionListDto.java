package gov.fdic.tip.emailmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DistributionListDto {

    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "Active|Inactive", message = "Status must be Active or Inactive")
    private String status;

    @NotEmpty(message = "At least one contact must be selected")
    private List<Long> contactIds = new ArrayList<>();

    private List<DistributionListMemberDto> members = new ArrayList<>();

    private long memberCount;
    private boolean inFlightSend;
    private boolean hasSendHistory;

    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;
}