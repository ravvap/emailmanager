package gov.fdic.tip.emailmanager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ContactDto {

    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please write a valid email address")
    private String email;

    @NotBlank(message = "Organization is required")
    private String organization;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "Active|Inactive", message = "Status must be Active or Inactive")
    private String status;

    @Valid
    private List<ContactAttributeValueDto> attributeValues = new ArrayList<>();

    private List<String> affectedDistributionLists = new ArrayList<>();

    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;
}