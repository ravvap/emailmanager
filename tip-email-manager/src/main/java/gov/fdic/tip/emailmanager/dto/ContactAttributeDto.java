package gov.fdic.tip.emailmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ContactAttributeDto {

    private Long id;

    @NotBlank(message = "Attribute Name is required")
    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "Text|Number|Date|Fixed List", message = "Type must be Text, Number, Date, or Fixed List")
    private String type;

    @Size(max = 500, message = "Default Value cannot exceed 500 characters")
    private String defaultValue;

    // List of allowed options when Type is 'Fixed List'
    private List<String> options;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "Active|Inactive", message = "Status must be Active or Inactive")
    private String status;

    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;
}