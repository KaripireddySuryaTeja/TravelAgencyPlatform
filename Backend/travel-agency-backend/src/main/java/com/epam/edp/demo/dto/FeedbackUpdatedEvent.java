package com.epam.edp.demo.dto;

import lombok.Getter;

/**
 * Spring application event published when a customer updates existing feedback.
 * Consumed by {@link com.epam.edp.demo.service.FeedbackEventListener} to re-run
 * moderation and reset the feedback status to PENDING before re-approval.
 */
@Getter
public class FeedbackUpdatedEvent {

    private final String feedbackId;
    private final String bookingId;
    private final String tourId;
    private final String customerId;
    private final int rating;
    private final String comment;

    public FeedbackUpdatedEvent(String feedbackId, String bookingId, String tourId,
                                String customerId, int rating, String comment) {
        this.feedbackId = feedbackId;
        this.bookingId = bookingId;
        this.tourId = tourId;
        this.customerId = customerId;
        this.rating = rating;
        this.comment = comment;
    }
}
