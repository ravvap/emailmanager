package gov.fdic.tip.emailmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContactAttributeValueDto {
    @NotNull(message = "Attribute ID is required")
    private Long attributeId;

    private String attributeName;

    @NotBlank(message = "Attribute value is required")
    private String value;
}