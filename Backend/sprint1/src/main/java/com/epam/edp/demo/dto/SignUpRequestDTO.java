package com.epam.edp.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@Schema(name = "SignUpRequest", description = "Payload used to register a new user account")
public class SignUpRequestDTO {

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be 1-50 characters")
    @Pattern(regexp = "^[\\p{L}\\p{M}' -]+$", message = "First name contains invalid characters")
    @Schema(description = "User first name", example = "Alex")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be 1-50 characters")
    @Pattern(regexp = "^[\\p{L}\\p{M}' -]+$", message = "Last name contains invalid characters")
    @Schema(description = "User last name", example = "Johnson")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a well-formed email address")
    @Size(max = 254, message = "Email is too long")
    @Schema(description = "Unique email address used for sign in", example = "alex.johnson@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be 8-64 characters")
    @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter")
    @Pattern(regexp = ".*[0-9].*", message = "Password must contain at least one number")
    @Pattern(regexp = ".*[^A-Za-z0-9].*", message = "Password must contain at least one special character")
    @Schema(description = "Password with at least one uppercase letter, one number, and one special character", example = "Travel@123")
    private String password;

    @Schema(description = "Google reCAPTCHA token from frontend widget", example = "03AGdBq24...")
    private String captchaToken;
}


