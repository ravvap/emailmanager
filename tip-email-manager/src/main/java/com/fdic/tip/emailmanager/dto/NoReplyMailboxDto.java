package com.fdic.tip.emailmanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Schema(description = "Data transfer object for No-Reply Mailbox configuration")
public class NoReplyMailboxDto {

	private Long id;  
	
    @NotBlank(message = "Email address is required.")
    @Email(message = "Email address not valid.")
    @Schema(description = "System-wide no-reply email address", example = "no-reply@fdic.gov")
    private String emailAddress;

    @Schema(description = "User who created the configuration", example = "admin_user")
    private String createdBy;

    @Schema(description = "Timestamp when the mailbox was configured")
    private ZonedDateTime createdAt;

    @Schema(description = "User who last updated the configuration", example = "admin_user")
    private String updatedBy;

    @Schema(description = "Timestamp when the mailbox was last updated")
    private ZonedDateTime updatedAt;
}