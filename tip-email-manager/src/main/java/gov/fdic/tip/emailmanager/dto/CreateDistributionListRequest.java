package gov.fdic.tip.emailmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class CreateDistributionListRequest {

    @NotBlank(message = "Distribution list name is required")
    private String name;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "Active|Inactive", message = "Status must be Active or Inactive")
    private String status;

    @NotEmpty(message = "At least one contact member must be selected when creating a distribution list")
    private List<Long> contactIds;
}