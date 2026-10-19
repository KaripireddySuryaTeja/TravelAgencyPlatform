package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.FeedbackRequest;
import com.epam.edp.demo.dto.FeedbackResponse;
import com.epam.edp.demo.dto.FeedbackUpdateRequest;
import com.epam.edp.demo.exception.FeedbackNotAllowedException;
import com.epam.edp.demo.exception.FeedbackNotFoundException;
import com.epam.edp.demo.service.FeedbackService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackControllerTest {

    @Mock
    private FeedbackService feedbackService;

    private FeedbackController feedbackController;

    @BeforeEach
    void setUp() {
        feedbackController = new FeedbackController(feedbackService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ─────────────────────────────────────────────
    // submitFeedback
    // ─────────────────────────────────────────────

    @Test
    void submitFeedback_returnsCreatedWithBody() {
        authenticate("u-1");
        FeedbackResponse expected = response("f-1", 5);
        when(feedbackService.submitFeedback(eq("b-1"), eq("u-1"), any(FeedbackRequest.class)))
                .thenReturn(expected);

        ResponseEntity<FeedbackResponse> result = feedbackController.submitFeedback("b-1", request(5, null));

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(expected, result.getBody());
    }

    @Test
    void submitFeedback_passesAuthenticatedUserIdToService() {
        authenticate("customer-42");
        FeedbackRequest req = request(4, null);
        when(feedbackService.submitFeedback(eq("b-1"), eq("customer-42"), any()))
                .thenReturn(response("f-1", 4));

        feedbackController.submitFeedback("b-1", req);

        verify(feedbackService).submitFeedback("b-1", "customer-42", req);
    }

    @Test
    void submitFeedback_propagatesNotAllowedException() {
        authenticate("u-1");
        when(feedbackService.submitFeedback(any(), any(), any()))
                .thenThrow(new FeedbackNotAllowedException("not allowed"));

        assertThrows(FeedbackNotAllowedException.class,
                () -> feedbackController.submitFeedback("b-1", request(5, null)));
    }

    @Test
    void submitFeedback_unauthorizedWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> feedbackController.submitFeedback("b-1", request(5, null)));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    // ─────────────────────────────────────────────
    // updateFeedback
    // ─────────────────────────────────────────────

    @Test
    void updateFeedback_returnsOkWithBody() {
        authenticate("u-1");
        FeedbackResponse expected = response("f-1", 3);
        when(feedbackService.updateFeedback(eq("b-1"), eq("u-1"), any(FeedbackUpdateRequest.class)))
                .thenReturn(expected);

        ResponseEntity<FeedbackResponse> result = feedbackController.updateFeedback("b-1", updateRequest(3, "ok"));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(expected, result.getBody());
    }

    @Test
    void updateFeedback_passesAuthenticatedUserIdToService() {
        authenticate("customer-7");
        FeedbackUpdateRequest req = updateRequest(4, null);
        when(feedbackService.updateFeedback(eq("b-1"), eq("customer-7"), any()))
                .thenReturn(response("f-1", 4));

        feedbackController.updateFeedback("b-1", req);

        verify(feedbackService).updateFeedback("b-1", "customer-7", req);
    }

    @Test
    void updateFeedback_propagatesFeedbackNotFoundException() {
        authenticate("u-1");
        when(feedbackService.updateFeedback(any(), any(), any()))
                .thenThrow(new FeedbackNotFoundException("not found"));

        assertThrows(FeedbackNotFoundException.class,
                () -> feedbackController.updateFeedback("b-1", updateRequest(5, null)));
    }

    @Test
    void updateFeedback_unauthorizedWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> feedbackController.updateFeedback("b-1", updateRequest(5, null)));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    // ─────────────────────────────────────────────
    // getFeedbackByBooking
    // ─────────────────────────────────────────────

    @Test
    void getFeedbackByBooking_returnsOkWithBody() {
        authenticate("u-1");
        FeedbackResponse expected = response("f-1", 5);
        when(feedbackService.getFeedbackByBooking("b-1", "u-1")).thenReturn(expected);

        ResponseEntity<FeedbackResponse> result = feedbackController.getFeedbackByBooking("b-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(expected, result.getBody());
    }

    @Test
    void getFeedbackByBooking_propagatesFeedbackNotFoundException() {
        authenticate("u-1");
        when(feedbackService.getFeedbackByBooking("b-1", "u-1"))
                .thenThrow(new FeedbackNotFoundException("no feedback"));

        assertThrows(FeedbackNotFoundException.class,
                () -> feedbackController.getFeedbackByBooking("b-1"));
    }

    @Test
    void getFeedbackByBooking_propagatesForbidden() {
        authenticate("u-1");
        when(feedbackService.getFeedbackByBooking("b-1", "u-1"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> feedbackController.getFeedbackByBooking("b-1"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getFeedbackByBooking_unauthorizedWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> feedbackController.getFeedbackByBooking("b-1"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    // ─────────────────────────────────────────────
    // getFeedbacksByTour
    // ─────────────────────────────────────────────

    @Test
    void getFeedbacksByTour_returnsOkWithList() {
        List<FeedbackResponse> expected = List.of(response("f-1", 5), response("f-2", 4));
        when(feedbackService.getFeedbacksByTour("t-1")).thenReturn(expected);

        ResponseEntity<List<FeedbackResponse>> result = feedbackController.getFeedbacksByTour("t-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().size());
    }

    @Test
    void getFeedbacksByTour_returnsEmptyListWhenNoFeedback() {
        when(feedbackService.getFeedbacksByTour("t-1")).thenReturn(List.of());

        ResponseEntity<List<FeedbackResponse>> result = feedbackController.getFeedbacksByTour("t-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().size());
    }

    @Test
    void getFeedbacksByTour_propagatesNotFoundForUnknownTour() {
        when(feedbackService.getFeedbacksByTour("t-unknown"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> feedbackController.getFeedbacksByTour("t-unknown"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getFeedbacksByTour_isPublicAndDoesNotRequireAuthentication() {
        // No authentication set — the endpoint should still work
        when(feedbackService.getFeedbacksByTour("t-1")).thenReturn(List.of());

        ResponseEntity<List<FeedbackResponse>> result = feedbackController.getFeedbacksByTour("t-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private static void authenticate(String userId) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(userId, null, "ROLE_USER");
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private static FeedbackResponse response(String id, int rating) {
        return FeedbackResponse.builder()
                .id(id)
                .rating(rating)
                .status("PENDING")
                .customerId("u-1")
                .bookingId("b-1")
                .tourId("t-1")
                .build();
    }

    private static FeedbackRequest request(int rating, String comment) {
        FeedbackRequest req = new FeedbackRequest();
        req.setRating(rating);
        req.setComment(comment);
        return req;
    }

    private static FeedbackUpdateRequest updateRequest(int rating, String comment) {
        FeedbackUpdateRequest req = new FeedbackUpdateRequest();
        req.setRating(rating);
        req.setComment(comment);
        return req;
    }
}
