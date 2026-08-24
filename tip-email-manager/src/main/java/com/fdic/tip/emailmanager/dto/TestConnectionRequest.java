package com.fdic.tip.emailmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestConnectionRequest {
    @NotBlank
    private String databaseLocation;

    @NotBlank
    private String vaultCredentialRef;
}