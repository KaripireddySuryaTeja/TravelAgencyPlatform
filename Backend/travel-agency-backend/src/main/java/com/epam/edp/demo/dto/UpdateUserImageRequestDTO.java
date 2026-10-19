package com.epam.edp.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserImageRequestDTO {

    @NotBlank(message = "Image data is required")
    private String imageBase64;
}

