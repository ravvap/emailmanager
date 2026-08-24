package com.fdic.tip.emailmanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO for data connection testing")
public class ConnectionTestResultDto {

    private boolean connected;
    private String message;
    private Long responseTimeMs;
}