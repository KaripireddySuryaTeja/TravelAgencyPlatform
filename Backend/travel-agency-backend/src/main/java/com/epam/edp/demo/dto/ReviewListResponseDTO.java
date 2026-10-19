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
@Schema(name = "ReviewListResponse", description = "Paginated reviews for a tour")
public class ReviewListResponseDTO {

    @Schema(description = "Reviews returned for the current page")
    private List<ReviewItem> reviews;
    @Schema(description = "Current page number", example = "1")
    private Integer page;
    @Schema(description = "Number of items per page", example = "4")
    private Integer pageSize;
    @Schema(description = "Total pages", example = "8")
    private Integer totalPages;
    @Schema(description = "Total reviews", example = "31")
    private Integer totalItems;
    @Schema(description = "Average rating across all reviews", example = "4.6")
    private Double averageRating;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "ReviewItem", description = "Single tour review entry")
    public static class ReviewItem {
        @Schema(description = "Review identifier", example = "6650d10b5c8f8d4f1ab34567")
        private String id;
        @Schema(description = "Display name of the reviewer", example = "Ananya K")
        private String userName;
        @Schema(description = "Reviewer avatar image URL")
        private String userAvatarUrl;
        @Schema(description = "Rating given by the user", example = "5.0")
        private Double rate;
        @Schema(description = "Review comment text")
        private String comment;
        @Schema(description = "Date when the review was submitted", example = "2026-04-12")
        private LocalDate reviewDate;
    }
}
