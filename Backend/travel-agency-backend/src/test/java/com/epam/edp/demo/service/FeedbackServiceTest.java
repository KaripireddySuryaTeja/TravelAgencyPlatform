package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.FeedbackRequest;
import com.epam.edp.demo.dto.FeedbackResponse;
import com.epam.edp.demo.dto.FeedbackSubmittedEvent;
import com.epam.edp.demo.dto.FeedbackUpdateRequest;
import com.epam.edp.demo.dto.FeedbackUpdatedEvent;
import com.epam.edp.demo.enums.BookingState;
import com.epam.edp.demo.enums.FeedbackStatus;
import com.epam.edp.demo.enums.ModerationStatus;
import com.epam.edp.demo.exception.ContentModerationException;
import com.epam.edp.demo.exception.FeedbackNotAllowedException;
import com.epam.edp.demo.exception.FeedbackNotFoundException;
import com.epam.edp.demo.exception.FeedbackRejectedException;
import com.epam.edp.demo.model.Booking;
import com.epam.edp.demo.model.Feedback;
import com.epam.edp.demo.model.Tour;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.repository.FeedbackRepository;
import com.epam.edp.demo.repository.TourRepository;
import com.epam.edp.demo.service.moderation.ModerationResult;
import com.epam.edp.demo.service.moderation.ModerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TourRepository tourRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ModerationService moderationService;

    private FeedbackService feedbackService;

    /** Default APPROVED moderation result – override in individual tests when needed. */
    private static final ModerationResult APPROVED =
            new ModerationResult(ModerationStatus.APPROVED, "Content is acceptable.");

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(
                feedbackRepository, bookingRepository, tourRepository,
                eventPublisher, moderationService);
    }

    // ─────────────────────────────────────────────
    // submitFeedback
    // ─────────────────────────────────────────────

    @Test
    void submitFeedback_bookingNotFound_throwsNotFound() {
        when(bookingRepository.findById("b-1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> feedbackService.submitFeedback("b-1", "u-1", request(4, null)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void submitFeedback_wrongCustomer_throwsForbidden() {
        Booking booking = booking("b-1", "owner-1", BookingState.STARTED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> feedbackService.submitFeedback("b-1", "other-customer", request(4, null)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void submitFeedback_bookingInBookedState_throwsNotAllowed() {
        Booking booking = booking("b-1", "u-1", BookingState.BOOKED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));

        assertThrows(FeedbackNotAllowedException.class,
                () -> feedbackService.submitFeedback("b-1", "u-1", request(4, null)));
    }

    @Test
    void submitFeedback_bookingCancelled_throwsNotAllowed() {
        Booking booking = booking("b-1", "u-1", BookingState.CANCELLED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));

        assertThrows(FeedbackNotAllowedException.class,
                () -> feedbackService.submitFeedback("b-1", "u-1", request(4, null)));
    }

    @Test
    void submitFeedback_ratingBelowThreeWithoutComment_throwsNotAllowed() {
        Booking booking = booking("b-1", "u-1", BookingState.STARTED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));

        assertThrows(FeedbackNotAllowedException.class,
                () -> feedbackService.submitFeedback("b-1", "u-1", request(2, null)));
    }

    @Test
    void submitFeedback_ratingThreeRequiresComment() {
        Booking booking = booking("b-1", "u-1", BookingState.STARTED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));

        assertThrows(FeedbackNotAllowedException.class,
                () -> feedbackService.submitFeedback("b-1", "u-1", request(3, "")));
    }

    @Test
    void submitFeedback_duplicateFeedback_throwsNotAllowed() {
        Booking booking = booking("b-1", "u-1", BookingState.STARTED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(feedbackRepository.existsByBookingId("b-1")).thenReturn(true);

        assertThrows(FeedbackNotAllowedException.class,
                () -> feedbackService.submitFeedback("b-1", "u-1", request(5, null)));
    }

    @Test
    void submitFeedback_validRequest_savesWithApprovedStatusAndPublishesEvent() {
        Booking booking = booking("b-1", "u-1", BookingState.STARTED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(feedbackRepository.existsByBookingId("b-1")).thenReturn(false);
        when(moderationService.moderate(any())).thenReturn(APPROVED);
        Feedback saved = feedback("f-1", "b-1", "tour-1", "u-1", 5, null);
        saved.setStatus(FeedbackStatus.APPROVED);
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(saved);

        FeedbackResponse response = feedbackService.submitFeedback("b-1", "u-1", request(5, null));

        assertNotNull(response);
        assertEquals("f-1", response.getId());
        assertEquals(5, response.getRating());
        assertEquals("APPROVED", response.getStatus());
        verify(eventPublisher).publishEvent(any(FeedbackSubmittedEvent.class));
    }

    @Test
    void submitFeedback_ratingFourOptionalComment_succeeds() {
        Booking booking = booking("b-1", "u-1", BookingState.FINISHED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(feedbackRepository.existsByBookingId("b-1")).thenReturn(false);
        when(moderationService.moderate(any())).thenReturn(APPROVED);
        Feedback saved = feedback("f-1", "b-1", "tour-1", "u-1", 4, null);
        saved.setStatus(FeedbackStatus.APPROVED);
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(saved);

        FeedbackResponse response = feedbackService.submitFeedback("b-1", "u-1", request(4, null));

        assertNotNull(response);
        assertEquals(4, response.getRating());
    }

    @Test
    void submitFeedback_oneFeedbackPerBooking_secondSubmitRejected() {
        Booking booking = booking("b-1", "u-1", BookingState.STARTED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(feedbackRepository.existsByBookingId("b-1")).thenReturn(true);

        FeedbackNotAllowedException ex = assertThrows(FeedbackNotAllowedException.class,
                () -> feedbackService.submitFeedback("b-1", "u-1", request(5, "Great trip!")));

        assertNotNull(ex.getMessage());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void submitFeedback_moderationFlagged_throwsFeedbackRejectedException() {
        Booking booking = booking("b-1", "u-1", BookingState.STARTED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(feedbackRepository.existsByBookingId("b-1")).thenReturn(false);
        ModerationResult flagged = new ModerationResult(ModerationStatus.FLAGGED,
                "Content contains hate speech.");
        when(moderationService.moderate(anyString())).thenReturn(flagged);

        FeedbackRejectedException ex = assertThrows(FeedbackRejectedException.class,
                () -> feedbackService.submitFeedback("b-1", "u-1",
                        request(5, "I hate this tour and all the people in it!")));

        assertEquals("Content contains hate speech.", ex.getReason());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void submitFeedback_moderationNeedsEdit_throwsContentModerationException() {
        Booking booking = booking("b-1", "u-1", BookingState.STARTED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(feedbackRepository.existsByBookingId("b-1")).thenReturn(false);
        ModerationResult needsEdit = new ModerationResult(ModerationStatus.NEEDS_EDIT,
                "Excessive profanity – please revise.");
        when(moderationService.moderate(anyString())).thenReturn(needsEdit);

        ContentModerationException ex = assertThrows(ContentModerationException.class,
                () -> feedbackService.submitFeedback("b-1", "u-1",
                        request(3, "This was f***ing terrible!!!")));

        assertEquals("NEEDS_EDIT", ex.getModerationStatus());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void submitFeedback_moderationServiceUnavailable_throwsContentModerationException() {
        Booking booking = booking("b-1", "u-1", BookingState.STARTED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(feedbackRepository.existsByBookingId("b-1")).thenReturn(false);
        when(moderationService.moderate(anyString())).thenThrow(
                new ContentModerationException("UNAVAILABLE",
                        "Azure OpenAI returned HTTP 503.",
                        "Content moderation is temporarily unavailable. Please try again later."));

        ContentModerationException ex = assertThrows(ContentModerationException.class,
                () -> feedbackService.submitFeedback("b-1", "u-1", request(5, "Great tour!")));

        assertEquals("UNAVAILABLE", ex.getModerationStatus());
        verify(feedbackRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────
    // updateFeedback
    // ─────────────────────────────────────────────

    @Test
    void updateFeedback_feedbackNotFound_throwsFeedbackNotFound() {
        Booking booking = booking("b-1", "u-1", BookingState.FINISHED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(feedbackRepository.findByBookingIdAndCustomerId("b-1", "u-1"))
                .thenReturn(Optional.empty());

        assertThrows(FeedbackNotFoundException.class,
                () -> feedbackService.updateFeedback("b-1", "u-1", updateRequest(4, null)));
    }

    @Test
    void updateFeedback_lowRatingWithoutComment_throwsNotAllowed() {
        Booking booking = booking("b-1", "u-1", BookingState.FINISHED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));

        assertThrows(FeedbackNotAllowedException.class,
                () -> feedbackService.updateFeedback("b-1", "u-1", updateRequest(1, null)));
    }

    @Test
    void updateFeedback_validRequest_setsApprovedAndPublishesEvent() {
        Booking booking = booking("b-1", "u-1", BookingState.FINISHED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        Feedback existing = feedback("f-1", "b-1", "tour-1", "u-1", 5, null);
        existing.setStatus(FeedbackStatus.APPROVED);
        when(feedbackRepository.findByBookingIdAndCustomerId("b-1", "u-1"))
                .thenReturn(Optional.of(existing));
        when(moderationService.moderate(anyString())).thenReturn(APPROVED);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        FeedbackResponse response = feedbackService.updateFeedback(
                "b-1", "u-1", updateRequest(3, "Could be better"));

        assertEquals("APPROVED", response.getStatus());
        assertEquals(3, response.getRating());
        verify(eventPublisher).publishEvent(any(FeedbackUpdatedEvent.class));
    }

    @Test
    void updateFeedback_setsNewRatingAndComment() {
        Booking booking = booking("b-1", "u-1", BookingState.FINISHED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        Feedback existing = feedback("f-1", "b-1", "tour-1", "u-1", 4, "Good");
        when(feedbackRepository.findByBookingIdAndCustomerId("b-1", "u-1"))
                .thenReturn(Optional.of(existing));
        when(moderationService.moderate(anyString())).thenReturn(APPROVED);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        feedbackService.updateFeedback("b-1", "u-1", updateRequest(2, "Not as expected"));
        verify(feedbackRepository).save(captor.capture());

        assertEquals(2, captor.getValue().getRating());
        assertEquals("Not as expected", captor.getValue().getComment());
        assertEquals(FeedbackStatus.APPROVED, captor.getValue().getStatus());
    }

    @Test
    void updateFeedback_moderationFlagged_throwsRejected_doesNotSave() {
        Booking booking = booking("b-1", "u-1", BookingState.FINISHED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        Feedback existing = feedback("f-1", "b-1", "tour-1", "u-1", 5, "Old feedback");
        when(feedbackRepository.findByBookingIdAndCustomerId("b-1", "u-1"))
                .thenReturn(Optional.of(existing));
        when(moderationService.moderate(anyString()))
                .thenReturn(new ModerationResult(ModerationStatus.FLAGGED, "Spam detected."));

        assertThrows(FeedbackRejectedException.class,
                () -> feedbackService.updateFeedback("b-1", "u-1",
                        updateRequest(4, "BUY CHEAP TICKETS AT SPAM.COM")));

        verify(feedbackRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────
    // getFeedbackByBooking
    // ─────────────────────────────────────────────

    @Test
    void getFeedbackByBooking_bookingNotFound_throwsNotFound() {
        when(bookingRepository.findById("b-1")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> feedbackService.getFeedbackByBooking("b-1", "u-1"));
    }

    @Test
    void getFeedbackByBooking_wrongCustomer_throwsForbidden() {
        Booking booking = booking("b-1", "owner-1", BookingState.FINISHED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> feedbackService.getFeedbackByBooking("b-1", "other"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getFeedbackByBooking_noFeedback_throwsFeedbackNotFound() {
        Booking booking = booking("b-1", "u-1", BookingState.FINISHED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(feedbackRepository.findByBookingId("b-1")).thenReturn(Optional.empty());

        assertThrows(FeedbackNotFoundException.class,
                () -> feedbackService.getFeedbackByBooking("b-1", "u-1"));
    }

    @Test
    void getFeedbackByBooking_returnsFeedback() {
        Booking booking = booking("b-1", "u-1", BookingState.FINISHED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        Feedback feedback = feedback("f-1", "b-1", "tour-1", "u-1", 5, "Excellent");
        when(feedbackRepository.findByBookingId("b-1")).thenReturn(Optional.of(feedback));

        FeedbackResponse response = feedbackService.getFeedbackByBooking("b-1", "u-1");

        assertEquals("f-1", response.getId());
        assertEquals(5, response.getRating());
        assertEquals("Excellent", response.getComment());
    }

    // ─────────────────────────────────────────────
    // getFeedbacksByTour
    // ─────────────────────────────────────────────

    @Test
    void getFeedbacksByTour_tourNotFound_throwsNotFound() {
        when(tourRepository.findById("t-1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> feedbackService.getFeedbacksByTour("t-1"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getFeedbacksByTour_returnsOnlyApprovedFeedbacks() {
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(new Tour()));
        Feedback approved = feedback("f-1", "b-1", "t-1", "u-1", 5, null);
        approved.setStatus(FeedbackStatus.APPROVED);
        when(feedbackRepository.findByTourIdAndStatus("t-1", FeedbackStatus.APPROVED))
                .thenReturn(List.of(approved));

        List<FeedbackResponse> responses = feedbackService.getFeedbacksByTour("t-1");

        assertEquals(1, responses.size());
        assertEquals("APPROVED", responses.get(0).getStatus());
    }

    @Test
    void getFeedbacksByTour_flaggedFeedbackNotReturned() {
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(new Tour()));
        when(feedbackRepository.findByTourIdAndStatus("t-1", FeedbackStatus.APPROVED))
                .thenReturn(List.of());

        List<FeedbackResponse> responses = feedbackService.getFeedbacksByTour("t-1");

        assertEquals(0, responses.size());
    }

    // ─────────────────────────────────────────────
    // validateFeedbackRules
    // ─────────────────────────────────────────────

    @Test
    void validateFeedbackRules_invalidRatingZero_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> feedbackService.validateFeedbackRules(0, "comment"));
    }

    @Test
    void validateFeedbackRules_invalidRatingSix_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> feedbackService.validateFeedbackRules(6, "comment"));
    }

    @Test
    void validateFeedbackRules_ratingOneWithComment_passes() {
        feedbackService.validateFeedbackRules(1, "Disappointing.");
    }

    @Test
    void validateFeedbackRules_ratingTwoWithoutComment_throwsNotAllowed() {
        assertThrows(FeedbackNotAllowedException.class,
                () -> feedbackService.validateFeedbackRules(2, null));
    }

    @Test
    void validateFeedbackRules_ratingFiveNoComment_passes() {
        feedbackService.validateFeedbackRules(5, null);
    }

    // ─────────────────────────────────────────────
    // moderateComment (legacy keyword-based)
    // ─────────────────────────────────────────────

    @Test
    void moderateComment_cleanComment_returnsApproved() {
        FeedbackStatus result = feedbackService.moderateComment("The tour was wonderful!");
        assertEquals(FeedbackStatus.APPROVED, result);
    }

    @Test
    void moderateComment_nullComment_returnsApproved() {
        FeedbackStatus result = feedbackService.moderateComment(null);
        assertEquals(FeedbackStatus.APPROVED, result);
    }

    @Test
    void moderateComment_bannedKeyword_returnsFlagged() {
        FeedbackStatus result = feedbackService.moderateComment("This is total spam!");
        assertEquals(FeedbackStatus.FLAGGED, result);
    }

    @Test
    void moderateComment_caseSensitivityIgnored_returnsFlagged() {
        FeedbackStatus result = feedbackService.moderateComment("What a SCAM!");
        assertEquals(FeedbackStatus.FLAGGED, result);
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private static Booking booking(String id, String userId, BookingState state) {
        Booking b = new Booking();
        b.setId(id);
        b.setUserId(userId);
        b.setTourId("tour-1");
        b.setState(state);
        return b;
    }

    private static Feedback feedback(String id, String bookingId, String tourId,
                                     String customerId, int rating, String comment) {
        Feedback f = new Feedback();
        f.setId(id);
        f.setBookingId(bookingId);
        f.setTourId(tourId);
        f.setCustomerId(customerId);
        f.setRating(rating);
        f.setComment(comment);
        f.setStatus(FeedbackStatus.PENDING);
        return f;
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