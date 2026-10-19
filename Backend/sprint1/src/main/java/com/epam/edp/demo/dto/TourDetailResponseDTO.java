package com.epam.edp.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "TourDetailResponse", description = "Complete detail view for a single tour")
public class TourDetailResponseDTO {

    @Schema(description = "Tour identifier", example = "6641b2ea5c8f8d4f1ab67890")
    private String id;
    @Schema(description = "Tour name", example = "Bali Explorer")
    private String name;
    @Schema(description = "Destination", example = "Bali")
    private String destination;
    @Schema(description = "Short marketing summary of the tour")
    private String summary;
    @Schema(description = "Gallery image URLs")
    private List<String> imageUrls;
    @Schema(description = "Average rating", example = "4.8")
    private Double rating;
    @Schema(description = "Number of reviews", example = "287")
    private Integer reviewCount;
    @Schema(description = "Available departure dates")
    private List<LocalDate> startDates;
    @Schema(description = "Available duration options")
    private List<String> durations;
    @Schema(description = "Price by duration option")
    private Map<String, String> pricePerDuration;
    @Schema(description = "Meal plan options")
    private List<String> mealPlans;
    @Schema(description = "Daily meal supplement pricing")
    private Map<String, String> mealSupplementsPerDay;
    @Schema(description = "Hotel name", example = "Ocean Pearl Resort")
    private String hotelName;
    @Schema(description = "Hotel description")
    private String hotelDescription;
    @Schema(description = "Accommodation type", example = "4-star beach resort")
    private String accommodation;
    @Schema(description = "Tour category", example = "ADVENTURE")
    private String tourType;
    @Schema(description = "Additional tour attributes and highlights")
    private Map<String, String> customDetails;
    @Schema(description = "Maximum supported guest counts")
    private GuestQuantityDTO guestQuantity;
    @Schema(description = "Number of days before departure eligible for free cancellation", example = "7")
    private Integer freeCancellationDaysBefore;
    @Schema(description = "Computed free cancellation deadline", example = "2026-08-08")
    private LocalDate freeCancellationDeadline;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "GuestQuantity", description = "Limits on guest counts for a tour")
    public static class GuestQuantityDTO {
        @Schema(description = "Maximum adults allowed", example = "4")
        private Integer adultsMaxValue;
        @Schema(description = "Maximum children allowed", example = "2")
        private Integer childrenMaxValue;
        @Schema(description = "Maximum total guests allowed", example = "5")
        private Integer totalMaxValue;
    }
}
