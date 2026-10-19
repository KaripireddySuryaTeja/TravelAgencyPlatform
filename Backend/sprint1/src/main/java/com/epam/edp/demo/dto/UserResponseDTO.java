package com.epam.edp.demo.dto;


import java.time.Instant;

import com.epam.edp.demo.enums.Role;
import com.epam.edp.demo.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserResponse", description = "Authenticated user profile information")
public record UserResponseDTO(
    @Schema(description = "User identifier", example = "663f2a2c5c8f8d4f1ab12345")
        String id,
    @Schema(description = "First name", example = "Alex")
        String firstName,
    @Schema(description = "Last name", example = "Johnson")
        String lastName,
    @Schema(description = "Email address", example = "alex.johnson@example.com")
        String email,
    @Schema(description = "Profile image URL")
        String imageUrl,
    @Schema(description = "User role", example = "CUSTOMER")
        Role role,
    @Schema(description = "Account creation timestamp", example = "2026-05-01T09:30:00Z")
        Instant createdAt
) {
    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(
                user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getImageUrl(), user.getRole(), user.getCreatedAt());

    }
}
