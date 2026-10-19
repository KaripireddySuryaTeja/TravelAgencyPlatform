package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.FeedbackSubmittedEvent;
import com.epam.edp.demo.dto.FeedbackUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for feedback domain events and performs post-persistence housekeeping.
 *
 * <p>As of sprint2, AI content moderation is performed <em>synchronously</em> inside
 * {@link FeedbackService} <em>before</em> persistence.  Feedback is saved to the
 * database only when the moderation status is {@code APPROVED}, so no re-moderation
 * step is needed here.</p>
 *
 * <p>This listener is intentionally kept for audit logging and can be extended
 * for future async post-processing (e.g. notifications, analytics).</p>
 */
@Component
public class FeedbackEventListener {

    private static final Logger log = LoggerFactory.getLogger(FeedbackEventListener.class);

    @Async
    @EventListener
    public void onFeedbackSubmitted(FeedbackSubmittedEvent event) {
        log.info("Feedback submitted and approved: id={}, booking={}, tour={}, rating={}",
                event.getFeedbackId(), event.getBookingId(),
                event.getTourId(), event.getRating());
    }

    @Async
    @EventListener
    public void onFeedbackUpdated(FeedbackUpdatedEvent event) {
        log.info("Feedback updated and approved: id={}, booking={}, tour={}, rating={}",
                event.getFeedbackId(), event.getBookingId(),
                event.getTourId(), event.getRating());
    }
}
