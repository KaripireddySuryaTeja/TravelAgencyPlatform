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
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.repository.FeedbackRepository;
import com.epam.edp.demo.repository.TourRepository;
import com.epam.edp.demo.service.moderation.ModerationResult;
import com.epam.edp.demo.service.moderation.ModerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private static final Set<BookingState> FEEDBACK_ALLOWED_STATES =
            Set.of(BookingState.STARTED, BookingState.FINISHED);

    private final FeedbackRepository feedbackRepository;
    private final BookingRepository bookingRepository;
    private final TourRepository tourRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ModerationService moderationService;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           BookingRepository bookingRepository,
                           TourRepository tourRepository,
                           ApplicationEventPublisher eventPublisher,
                           ModerationService moderationService) {
        this.feedbackRepository = feedbackRepository;
        this.bookingRepository = bookingRepository;
        this.tourRepository = tourRepository;
        this.eventPublisher = eventPublisher;
        this.moderationService = moderationService;
    }

    // ─────────────────────────────────────────────
    // Submit feedback (one per booking)
    // ─────────────────────────────────────────────

    /**
     * Submits new feedback for a booking.
     *
     * <p>AI content moderation is performed <em>synchronously before saving</em>.
     * Feedback is only persisted when the moderation status is {@code APPROVED}.
     * {@code NEEDS_EDIT} throws {@link ContentModerationException} (HTTP 422) and
     * {@code FLAGGED} throws {@link FeedbackRejectedException} (HTTP 422).</p>
     */
    public FeedbackResponse submitFeedback(String bookingId, String customerId, FeedbackRequest request) {
        Booking booking = resolveBooking(bookingId);
        validateOwnership(booking, customerId);
        validateBookingStateAllowsFeedback(booking);
        validateFeedbackRules(request.getRating(), request.getComment());

        if (feedbackRepository.existsByBookingId(bookingId)) {
            throw new FeedbackNotAllowedException(
                    "Feedback already submitted for booking " + bookingId + ". Use PUT to update.");
        }

        // ── AI Moderation (synchronous – content must pass before being saved) ──
        ModerationResult moderation = moderationService.moderate(request.getComment());
        enforceModeration(moderation);

        Feedback feedback = new Feedback();
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedback.setStatus(FeedbackStatus.APPROVED);   // persisted only when APPROVED
        feedback.setCustomerId(customerId);
        feedback.setBookingId(bookingId);
        feedback.setTourId(booking.getTourId());

        feedback = feedbackRepository.save(feedback);
        log.info("Feedback saved: id={}, status=APPROVED, booking={}", feedback.getId(), bookingId);

        eventPublisher.publishEvent(new FeedbackSubmittedEvent(
                feedback.getId(), bookingId, booking.getTourId(),
                customerId, feedback.getRating(), feedback.getComment()));

        return toResponse(feedback);
    }

    // ─────────────────────────────────────────────
    // Update feedback
    // ─────────────────────────────────────────────

    /**
     * Updates existing feedback.  AI moderation is re-run on the updated content.
     */
    public FeedbackResponse updateFeedback(String bookingId, String customerId, FeedbackUpdateRequest request) {
        Booking booking = resolveBooking(bookingId);
        validateOwnership(booking, customerId);
        validateBookingStateAllowsFeedback(booking);
        validateFeedbackRules(request.getRating(), request.getComment());

        Feedback feedback = feedbackRepository.findByBookingIdAndCustomerId(bookingId, customerId)
                .orElseThrow(() -> new FeedbackNotFoundException(
                        "No feedback found for booking " + bookingId));

        // ── AI Moderation ──────────────────────────────────────────────────────
        ModerationResult moderation = moderationService.moderate(request.getComment());
        enforceModeration(moderation);

        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedback.setStatus(FeedbackStatus.APPROVED);   // updated content is also APPROVED

        feedback = feedbackRepository.save(feedback);
        log.info("Feedback updated: id={}, status=APPROVED, booking={}", feedback.getId(), bookingId);

        eventPublisher.publishEvent(new FeedbackUpdatedEvent(
                feedback.getId(), bookingId, booking.getTourId(),
                customerId, feedback.getRating(), feedback.getComment()));

        return toResponse(feedback);
    }

    // ─────────────────────────────────────────────
    // Get feedback for a specific booking
    // ─────────────────────────────────────────────
    public FeedbackResponse getFeedbackByBooking(String bookingId, String customerId) {
        Booking booking = resolveBooking(bookingId);
        validateOwnership(booking, customerId);

        return feedbackRepository.findByBookingId(bookingId)
                .map(this::toResponse)
                .orElseThrow(() -> new FeedbackNotFoundException(
                        "No feedback found for booking " + bookingId));
    }

    // ─────────────────────────────────────────────
    // Get approved feedbacks for a tour (public)
    // ─────────────────────────────────────────────
    public List<FeedbackResponse> getFeedbacksByTour(String tourId) {
        tourRepository.findById(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour not found: " + tourId));

        return feedbackRepository.findByTourIdAndStatus(tourId, FeedbackStatus.APPROVED)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // Validation helpers
    // ─────────────────────────────────────────────

    /**
     * Validates rating range and mandatory comment rule.
     * Rating must be 1–5; comment is required when rating <= 3.
     */
    public void validateFeedbackRules(int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        if (rating <= 3 && (comment == null || comment.isBlank())) {
            throw new FeedbackNotAllowedException(
                    "A comment is required when the rating is 3 or below");
        }
    }

    /**
     * Legacy keyword-based moderation — kept for backward compatibility.
     * The AI moderation ({@link ModerationService}) is now the primary gate.
     *
     * @deprecated Replaced by AI moderation in {@link ModerationServiceImpl}.
     */
    @Deprecated(since = "sprint2", forRemoval = true)
    public FeedbackStatus moderateComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return FeedbackStatus.APPROVED;
        }
        String lower = comment.toLowerCase();
        List<String> bannedKeywords = List.of(
                "spam", "scam", "fraud", "fake", "hate", "racist", "sexist",
                "offensive", "abuse", "violent", "idiot", "stupid", "moron",
                "damn", "crap", "shit", "fuck", "bitch", "ass");
        boolean flagged = bannedKeywords.stream().anyMatch(lower::contains);
        return flagged ? FeedbackStatus.FLAGGED : FeedbackStatus.APPROVED;
    }

    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    /**
     * Enforces a moderation decision.
     *
     * <ul>
     *   <li>{@code APPROVED}   → proceeds normally (no-op).</li>
     *   <li>{@code NEEDS_EDIT} → throws {@link ContentModerationException} (HTTP 422).</li>
     *   <li>{@code FLAGGED}    → throws {@link FeedbackRejectedException} (HTTP 422).</li>
     * </ul>
     */
    private void enforceModeration(ModerationResult moderation) {
        if (moderation.status() == ModerationStatus.NEEDS_EDIT) {
            log.warn("Moderation NEEDS_EDIT: {}", moderation.reason());
            throw new ContentModerationException(
                    "NEEDS_EDIT",
                    moderation.reason(),
                    "Please revise your feedback: " + moderation.reason());
        }
        if (moderation.status() == ModerationStatus.FLAGGED) {
            log.warn("Moderation FLAGGED: {}", moderation.reason());
            throw new FeedbackRejectedException(moderation.reason());
        }
        // APPROVED – fall through
        log.debug("Moderation APPROVED: {}", moderation.reason());
    }

    private Booking resolveBooking(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Booking not found: " + bookingId));
    }

    private void validateOwnership(Booking booking, String customerId) {
        if (!booking.getUserId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only manage feedback for your own bookings");
        }
    }

    private void validateBookingStateAllowsFeedback(Booking booking) {
        if (!FEEDBACK_ALLOWED_STATES.contains(booking.getState())) {
            throw new FeedbackNotAllowedException(
                    "Feedback is only allowed when the booking is in STARTED or FINISHED state. "
                            + "Current state: " + booking.getState());
        }
    }

    private FeedbackResponse toResponse(Feedback f) {
        return FeedbackResponse.builder()
                .id(f.getId())
                .rating(f.getRating())
                .comment(f.getComment())
                .status(f.getStatus() != null ? f.getStatus().name() : null)
                .customerId(f.getCustomerId())
                .bookingId(f.getBookingId())
                .tourId(f.getTourId())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}
