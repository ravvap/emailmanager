package com.fdic.tip.emailmanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Schema(description = "Data transfer object for Internal Domain Allowlist entry")
public class DomainAllowlistDto {

    @Schema(description = "Unique identifier of the allowed domain entry", example = "1")
    private Long id;

    @NotBlank(message = "Domain is required.")
 // Requires standard domain pattern (e.g., domain.com, sub.domain.gov)
    @Pattern(
        regexp = "^(?i)[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}$", 
        message = "Invalid domain format."
    )
    @Schema(description = "Allowed email domain name", example = "fdic.gov")
    private String domain;

    @NotBlank(message = "Status is required.")
    @Schema(description = "Status of the domain entry (ACTIVE/INACTIVE)", example = "ACTIVE")
    private String status;

    @Schema(description = "User who added the domain", example = "SYSTEM_SEED")
    private String createdBy;

    @Schema(description = "Timestamp when the domain was added")
    private ZonedDateTime createdAt;

    @Schema(description = "User who last updated the domain entry", example = "admin_user")
    private String updatedBy;

    @Schema(description = "Timestamp when the entry was last updated")
    private ZonedDateTime updatedAt;
}