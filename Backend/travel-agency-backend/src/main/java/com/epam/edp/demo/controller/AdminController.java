package com.epam.edp.demo.controller;

import com.epam.edp.demo.model.Review;
import com.epam.edp.demo.repository.ReviewRepository;
import com.epam.edp.demo.repository.TourRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// All endpoints under /api/v1/admin/** require ROLE_ADMIN (enforced in SecurityConfig)
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final ReviewRepository reviewRepository;
    private final TourRepository tourRepository;

    public AdminController(ReviewRepository reviewRepository, TourRepository tourRepository) {
        this.reviewRepository = reviewRepository;
        this.tourRepository = tourRepository;
    }

    /**
     * GET /api/v1/admin/reviews
     * Returns all reviews (visible and hidden) for moderation.
     * Optional filter: ?tourId=xxx
     */
    @GetMapping("/reviews")
    public ResponseEntity<List<ReviewModerationDTO>> getAllReviews(
            @RequestParam(required = false) String tourId
    ) {
        List<Review> reviews = tourId != null && !tourId.isBlank()
                ? reviewRepository.findByTourId(tourId)
                : reviewRepository.findAll();

        List<ReviewModerationDTO> result = reviews.stream()
                .map(r -> {
                    String tourName = tourRepository.findById(r.getTourId())
                            .map(t -> t.getName()).orElse("Unknown Tour");
                    return new ReviewModerationDTO(
                            r.getId(), r.getTourId(), tourName,
                            r.getUserName(), r.getRate(), r.getComment(),
                            r.getReviewDate(), r.isHidden()
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * PATCH /api/v1/admin/reviews/{id}/visibility
     * Body: {"hidden": true} or {"hidden": false}
     */
    @PatchMapping("/reviews/{id}/visibility")
    public ResponseEntity<Map<String, Object>> setReviewVisibility(
            @PathVariable String id,
            @RequestParam boolean hidden
    ) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found: " + id));
        review.setHidden(hidden);
        reviewRepository.save(review);
        return ResponseEntity.ok(Map.of(
                "id", id,
                "hidden", hidden,
                "message", hidden ? "Review hidden from customers" : "Review is now visible"
        ));
    }

    // ─── Response DTO ──────────────────────────────────────────────────────────
    public record ReviewModerationDTO(
            String id,
            String tourId,
            String tourName,
            String userName,
            Double rate,
            String comment,
            LocalDate reviewDate,
            boolean hidden
    ) {}
}
