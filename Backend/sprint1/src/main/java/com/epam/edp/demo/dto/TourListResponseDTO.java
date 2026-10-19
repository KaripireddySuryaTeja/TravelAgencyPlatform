package com.epam.edp.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "TourListResponse", description = "Paginated list of available tours")
public class TourListResponseDTO {

    @Schema(description = "Tours returned for the current page")
    private List<TourItem> tours;
    @Schema(description = "Current page number", example = "1")
    private Integer page;
    @Schema(description = "Number of items per page", example = "6")
    private Integer pageSize;
    @Schema(description = "Total number of pages", example = "4")
    private Integer totalPages;
    @Schema(description = "Total number of matching tours", example = "24")
    private Integer totalItems;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "TourListItem", description = "Compact representation of a tour in search results")
    public static class TourItem {
        @Schema(description = "Tour identifier", example = "6641b2ea5c8f8d4f1ab67890")
        private String id;
        @Schema(description = "Display name of the tour", example = "Bali Explorer")
        private String name;
        @Schema(description = "Destination of the tour", example = "Bali")
        private String destination;
        @Schema(description = "Nearest available start date", example = "2026-08-15")
        private LocalDate startDate;
        @Schema(description = "Available duration options")
        private List<String> durations;
        @Schema(description = "Available meal plan options")
        private List<String> mealPlans;
        @Schema(description = "Starting price or computed display price", example = "$1499")
        private String price;
        @Schema(description = "Average rating", example = "4.7")
        private Double rating;
        @Schema(description = "Number of reviews", example = "132")
        private Integer reviews;
        @Schema(description = "Free cancellation deadline for the displayed departure", example = "2026-08-10")
        private LocalDate freeCancellation;
    }
}
