package com.epam.edp.demo.dto;

import lombok.Getter;

/**
 * Spring application event published when a customer submits new feedback.
 * Consumed by {@link com.epam.edp.demo.service.FeedbackEventListener} to run
 * asynchronous moderation and update the feedback status.
 */
@Getter
public class FeedbackSubmittedEvent {

    private final String feedbackId;
    private final String bookingId;
    private final String tourId;
    private final String customerId;
    private final int rating;
    private final String comment;

    public FeedbackSubmittedEvent(String feedbackId, String bookingId, String tourId,
                                  String customerId, int rating, String comment) {
        this.feedbackId = feedbackId;
        this.bookingId = bookingId;
        this.tourId = tourId;
        this.customerId = customerId;
        this.rating = rating;
        this.comment = comment;
    }
}
