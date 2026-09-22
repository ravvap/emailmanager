package gov.fdic.tip.emailmanager.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating or editing a Data Source Query.
 *
 * @author prasad ravva
 */
public record CreateQueryRequest(
    @NotBlank(message = "Query name is required") String name,
    @NotBlank(message = "Bound connection ID is required") String connectionId,
    @NotBlank(message = "SQL configuration is required") String sqlText,
    String parameters
) {}