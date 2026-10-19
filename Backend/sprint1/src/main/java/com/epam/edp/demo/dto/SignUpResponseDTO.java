package com.epam.edp.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SignUpResponse", description = "Registration result message")
public record SignUpResponseDTO(String message) {
    public static SignUpResponseDTO ok() {
        return new SignUpResponseDTO("Account created successfully");
    }
}

