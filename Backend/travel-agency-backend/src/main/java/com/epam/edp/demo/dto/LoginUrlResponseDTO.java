package com.epam.edp.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO returning the OAuth2 login initiation URL for a given provider.
 * Frontend uses this URL to redirect the user to the provider's login page.
 */
@Schema(name = "LoginUrlResponse", description = "OAuth2 social login initiation URL")
public record LoginUrlResponseDTO(
        @Schema(description = "OAuth2 provider name", example = "google")
        String provider,

        @Schema(description = "URL to initiate OAuth2 login flow",
                example = "http://localhost:8080/oauth2/authorization/google")
        String loginUrl
) {}
