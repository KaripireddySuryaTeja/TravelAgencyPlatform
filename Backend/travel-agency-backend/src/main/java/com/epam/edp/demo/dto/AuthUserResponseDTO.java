package com.epam.edp.demo.dto;

import com.epam.edp.demo.enums.AuthProvider;
import com.epam.edp.demo.enums.Role;
import com.epam.edp.demo.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for authenticated user details (used by /auth/me and OAuth2 flows).
 * Password is never included in this response.
 */
@Schema(name = "AuthUserResponse", description = "Authenticated user profile details")
public record AuthUserResponseDTO(
        @Schema(description = "User identifier", example = "663f2a2c5c8f8d4f1ab12345")
        String id,

        @Schema(description = "First name", example = "John")
        String firstName,

        @Schema(description = "Last name", example = "Doe")
        String lastName,

        @Schema(description = "Email address", example = "john.doe@gmail.com")
        String email,

        @Schema(description = "User role", example = "CUSTOMER")
        Role role,

        @Schema(description = "Authentication provider", example = "GOOGLE")
        AuthProvider provider,

        @Schema(description = "Profile image URL")
        String imageUrl
) {
    public static AuthUserResponseDTO from(User user) {
        return new AuthUserResponseDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getProvider(),
                user.getImageUrl()
        );
    }
}
