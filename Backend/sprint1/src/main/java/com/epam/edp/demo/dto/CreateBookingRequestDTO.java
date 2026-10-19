package com.epam.edp.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
@Schema(name = "CreateBookingRequest", description = "Payload used to create a booking for the authenticated user")
public class CreateBookingRequestDTO {

    @NotBlank(message = "User ID is required")
    @Schema(description = "User identifier. Must match the authenticated user token.", example = "663f2a2c5c8f8d4f1ab12345")
    private String userId;

    @NotBlank(message = "Tour ID is required")
    @Schema(description = "Tour identifier selected by the user", example = "6641b2ea5c8f8d4f1ab67890")
    private String tourId;

    @NotNull(message = "Date is required")
    @Schema(description = "Requested travel start date", example = "2026-08-15")
    private LocalDate date;

    @NotBlank(message = "Duration is required")
    @Schema(description = "Selected duration option for the tour", example = "7 days / 6 nights")
    private String duration;

    @NotBlank(message = "Meal plan is required")
    @Pattern(regexp = "BB|HB|FB|AI|RO", message = "Invalid meal plan. Allowed values: BB, HB, FB, AI, RO")
    @Schema(description = "Selected meal plan for the booking", example = "ALL_INCLUSIVE")
    private String mealPlan;

    @NotNull(message = "Guests are required")
    @Valid
    @Schema(description = "Counts of adults and children traveling")
    private BookingGuestsDTO guests;

    @NotNull(message = "Personal details are required")
    @Size(min = 1, message = "At least one person's details are required")
    @Valid
    @Schema(description = "Personal details for each traveler included in the booking")
    private List<PersonalDetailDTO> personalDetails;
}

