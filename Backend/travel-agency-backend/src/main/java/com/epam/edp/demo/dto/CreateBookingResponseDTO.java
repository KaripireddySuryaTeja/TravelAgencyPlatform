package com.epam.edp.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateBookingResponse", description = "Summary returned after booking creation")
public class CreateBookingResponseDTO {

    @Schema(description = "Last date eligible for free cancellation", example = "2026-08-10")
    private LocalDate freeCancelation;
    @Schema(description = "Human-readable booking summary", example = "Booking confirmed successfully")
    private String details;
}

