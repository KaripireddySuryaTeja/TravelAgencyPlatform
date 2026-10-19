package com.epam.edp.demo.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shared guests payload used by both CreateBookingRequestDTO
 * and UpdateBookingRequestDTO to avoid class duplication.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "BookingGuests", description = "Guest counts for a booking")
public class BookingGuestsDTO {
    @Min(value = 1, message = "At least 1 adult is required")
    @Schema(description = "Number of adult travelers", example = "2", minimum = "1")
    private int adult = 1;

    @Min(value = 0, message = "Children count cannot be negative")
    @Schema(description = "Number of child travelers", example = "1", minimum = "0")
    private int children = 0;
}

