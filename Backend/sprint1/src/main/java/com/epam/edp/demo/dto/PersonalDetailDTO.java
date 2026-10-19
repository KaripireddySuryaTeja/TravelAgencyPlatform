package com.epam.edp.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shared personal-detail entry used by both CreateBookingRequestDTO
 * and UpdateBookingRequestDTO to avoid class duplication.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "PersonalDetail", description = "Traveler identity details for each person in the booking")
public class PersonalDetailDTO {

    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "First name must contain only letters, spaces, hyphens, and apostrophes")
    @Schema(description = "Traveler first name", example = "Alex")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "Last name must contain only letters, spaces, hyphens, and apostrophes")
    @Schema(description = "Traveler last name", example = "Johnson")
    private String lastName;
}

