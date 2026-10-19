package com.epam.edp.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic message response DTO used for profile update operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDTO {
    private String message;

    public static MessageResponseDTO of(String message) {
        return new MessageResponseDTO(message);
    }
}

