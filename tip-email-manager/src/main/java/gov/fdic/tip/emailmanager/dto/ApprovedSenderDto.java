package gov.fdic.tip.emailmanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ApprovedSenderDto {

    private Long id;

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotBlank(message = "Mailbox address is required")
    @Email(message = "Invalid email format")
    private String mailboxAddress;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "Status must be ACTIVE or INACTIVE")
    private String status;

    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;
}