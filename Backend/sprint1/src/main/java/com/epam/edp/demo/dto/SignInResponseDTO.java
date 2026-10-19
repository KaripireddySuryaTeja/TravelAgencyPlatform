package com.epam.edp.demo.dto;

import java.time.Instant;

import com.epam.edp.demo.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SignInResponse", description = "Authentication result containing a JWT token and user context")
public record SignInResponseDTO(
        @Schema(description = "JWT bearer token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2NjNmMmEyYzVjOGY4ZDRmMWFiMTIzNDUifQ.signature")
        String idToken,
        @Schema(description = "Role assigned to the user", example = "USER")
        Role role,
        @Schema(description = "Display name of the user", example = "Alex Johnson")
        String userName,
        @Schema(description = "User email", example = "alex.johnson@example.com")
        String email,
        @Schema(description = "Token expiry timestamp", example = "2026-05-12T10:15:30Z")
        Instant expiresAt
) {}

