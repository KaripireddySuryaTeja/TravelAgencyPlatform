package com.epam.edp.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmEmailRequestDTO {

    @NotBlank(message = "Confirmation token is required")
    private String confirmationToken;
}

