package com.epam.edp.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@Schema(name = "SignInRequest", description = "Credentials used to obtain a JWT access token")
public class SignInRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a well-formed email address")
    @Schema(description = "Registered account email", example = "alex.johnson@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "Account password", example = "Travel@123")
    private String password;
}

