package com.epam.edp.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(name = "UpdateBookingRequest", description = "Payload used to update editable booking fields")
public class UpdateBookingRequestDTO {

    @Schema(description = "Updated travel date", example = "2026-08-18")
    private LocalDate date;

    @Schema(description = "Updated duration option", example = "10 days / 9 nights")
    private String duration;

    @Schema(description = "Updated meal plan", example = "BREAKFAST_ONLY")
    private String mealPlan;

    @Valid
    @Schema(description = "Updated guest counts")
    private BookingGuestsDTO guests;

    @Valid
    @Size(min = 1, message = "At least one person's details are required")
    @Schema(description = "Updated traveler list")
    private List<PersonalDetailDTO> personalDetails;
}
