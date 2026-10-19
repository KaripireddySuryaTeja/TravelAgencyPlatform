package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.FeedbackRequest;
import com.epam.edp.demo.dto.FeedbackResponse;
import com.epam.edp.demo.dto.FeedbackUpdateRequest;
import com.epam.edp.demo.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * POST /api/v1/bookings/{bookingId}/feedback
     * Submit feedback for a booking. Authenticated customer only.
     */
    @PostMapping("/api/v1/bookings/{bookingId}/feedback")
    public ResponseEntity<FeedbackResponse> submitFeedback(
            @PathVariable String bookingId,
            @Valid @RequestBody FeedbackRequest request
    ) {
        String customerId = getAuthenticatedUserId();
        FeedbackResponse response = feedbackService.submitFeedback(bookingId, customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/v1/bookings/{bookingId}/feedback
     * Update existing feedback for a booking. Authenticated customer only.
     */
    @PutMapping("/api/v1/bookings/{bookingId}/feedback")
    public ResponseEntity<FeedbackResponse> updateFeedback(
            @PathVariable String bookingId,
            @Valid @RequestBody FeedbackUpdateRequest request
    ) {
        String customerId = getAuthenticatedUserId();
        FeedbackResponse response = feedbackService.updateFeedback(bookingId, customerId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/bookings/{bookingId}/feedback
     * Retrieve feedback for a specific booking. Authenticated customer only.
     */
    @GetMapping("/api/v1/bookings/{bookingId}/feedback")
    public ResponseEntity<FeedbackResponse> getFeedbackByBooking(
            @PathVariable String bookingId
    ) {
        String customerId = getAuthenticatedUserId();
        FeedbackResponse response = feedbackService.getFeedbackByBooking(bookingId, customerId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/tours/{tourId}/feedback
     * List all approved feedbacks for a tour. Public endpoint.
     */
    @GetMapping("/api/v1/tours/{tourId}/feedback")
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksByTour(
            @PathVariable String tourId
    ) {
        List<FeedbackResponse> responses = feedbackService.getFeedbacksByTour(tourId);
        return ResponseEntity.ok(responses);
    }

    // ─────────────────────────────────────────────
    // Helper: extract userId from JWT context
    // ─────────────────────────────────────────────
    private String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return auth.getName();
    }
}
