package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.DestinationListResponseDTO;
import com.epam.edp.demo.dto.ReviewListResponseDTO;
import com.epam.edp.demo.dto.TourDetailResponseDTO;
import com.epam.edp.demo.dto.TourListResponseDTO;
import com.epam.edp.demo.dto.ApiErrorResponseDTO;
import com.epam.edp.demo.service.TourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/tours")
@Tag(name = "Tours", description = "Tour discovery and details endpoints")
public class TourController {

    private final TourService tourService;

    public TourController(TourService tourService) {
        this.tourService = tourService;
    }

    @GetMapping("/destinations")
        @Operation(summary = "Search destinations", description = "Returns destination suggestions matching the provided query.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Destinations retrieved",
                content = @Content(schema = @Schema(implementation = DestinationListResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class)))
        })
    public ResponseEntity<DestinationListResponseDTO> getDestinations(
            @Parameter(description = "Destination search text", required = true, example = "Bali")
            @RequestParam String destination
    ) {
        DestinationListResponseDTO response =
                tourService.searchDestinations(destination);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
        @Operation(summary = "Get available tours", description = "Returns a paginated list of tours filtered by optional search criteria.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tours retrieved",
                content = @Content(schema = @Schema(implementation = TourListResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class)))
        })
    public ResponseEntity<TourListResponseDTO> getAvailableTours(
            @Parameter(description = "Destination filter", example = "Paris")
            @RequestParam(required = false) String destination,
            @Parameter(description = "Trip start date (ISO-8601)", example = "2026-07-01")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Trip end date (ISO-8601)", example = "2026-07-10")
            @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "Duration filter text", example = "7-10 days")
            @RequestParam(required = false) String duration,
            @Parameter(description = "Number of adults", example = "2")
            @RequestParam(required = false) Integer adults,
            @Parameter(description = "Number of children", example = "1")
            @RequestParam(required = false) Integer children,
            @Parameter(description = "Meal plan filter", example = "ALL_INCLUSIVE")
            @RequestParam(required = false) String mealPlan,
            @Parameter(description = "Tour type filter", example = "ADVENTURE")
            @RequestParam(required = false) String tourType,
            @Parameter(description = "Sorting strategy", example = "RATING_DESC")
            @RequestParam(defaultValue = "RATING_DESC") String sortBy,
            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size", example = "6")
            @RequestParam(defaultValue = "6") int pageSize
    ) {
        TourListResponseDTO response = tourService.getAvailableTours(
                destination,
                startDate,
                endDate,
                duration,
                adults,
                children,
                mealPlan,
                tourType,
                sortBy,
                page,
                pageSize
        );

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<TourListResponseDTO> getAvailableTours(
            String destination,
            LocalDate startDate,
            String duration,
            Integer adults,
            Integer children,
            String mealPlan,
            String tourType,
            String sortBy,
            int page,
            int pageSize
    ) {
        return getAvailableTours(
                destination,
                startDate,
                null,
                duration,
                adults,
                children,
                mealPlan,
                tourType,
                sortBy,
                page,
                pageSize
        );
    }

    @GetMapping("/{id}")
        @Operation(summary = "Get tour by id", description = "Returns complete details for a tour.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tour found",
                content = @Content(schema = @Schema(implementation = TourDetailResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Tour not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class)))
        })
    public ResponseEntity<TourDetailResponseDTO> getTourById(@PathVariable String id) {
        return ResponseEntity.ok(tourService.getTourById(id));
    }

    @GetMapping("/{id}/reviews")
        @Operation(summary = "Get tour reviews", description = "Returns paginated reviews for a given tour.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reviews retrieved",
                content = @Content(schema = @Schema(implementation = ReviewListResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Tour not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class)))
        })
    public ResponseEntity<ReviewListResponseDTO> getReviews(
            @Parameter(description = "Tour identifier", required = true, example = "6641b2ea5c8f8d4f1ab67890")
            @PathVariable String id,
            @Parameter(description = "Review sorting strategy", example = "TOP_RATED_FIRST")
            @RequestParam(defaultValue = "TOP_RATED_FIRST") String sortBy,
            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size", example = "4")
            @RequestParam(defaultValue = "4") int pageSize
    ) {
        return ResponseEntity.ok(tourService.getReviews(id, sortBy, page, pageSize));
    }
}