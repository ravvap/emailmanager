package com.fdic.tip.emailmanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.Set;

@Data
@Schema(description = "Data transfer object for Data Connection information")
public class DataConnectionDto {

    @Schema(description = "Unique identifier of the connection", example = "101")
    private Long id;

    @NotBlank(message = "Connection Name is required.")
    @Schema(description = "Unique name of the data connection", example = "SQL_SERVER_MAIN")
    private String name;

    @Schema(description = "Detailed description of the connection purpose", example = "Primary transactional database connection")
    private String description;

    @NotNull(message = "Database Location is required.")
    @Schema(description = "ID of the associated Database Location", example = "5")
    private Long databaseLocationId;

    @NotBlank(message = "Status is required.")
    @Schema(description = "Status of the connection (ACTIVE/INACTIVE)", example = "ACTIVE")
    private String status;

    @Schema(description = "Indicates if the connection is currently linked to an email template", example = "false")
    private boolean isUsedInTemplate;

    @Schema(description = "Set of User IDs assigned as authors for this connection", example = "[12, 45, 89]")
    private Set<Long> authorUserIds;

    @Schema(description = "User who created the record", example = "admin_user")
    private String createdBy;

    @Schema(description = "Timestamp when the record was created")
    private ZonedDateTime createdAt;

    @Schema(description = "User who last updated the record", example = "editor_user")
    private String updatedBy;

    @Schema(description = "Timestamp when the record was last updated")
    private ZonedDateTime updatedAt;
    
    @Schema(description = "Optional password used strictly for local connection testing overrides", nullable = true)
    private String password;
}