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
@Schema(description = "Dropdown projection for eligible connection authors")
public class AuthorDropdownDto {

    private Long id;
    private String username;
    private String displayName;
    private String emailAddress;
}