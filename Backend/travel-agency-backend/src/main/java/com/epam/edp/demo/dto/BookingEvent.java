package com.epam.edp.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Event payload published to RabbitMQ on every booking state change.
 * Consumed by the report-app to build weekly statistics.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEvent {

    /** BOOKING_CREATED | BOOKING_CANCELLED | BOOKING_FINISHED */
    private String eventType;

    private String bookingId;
    private String tourId;
    private String tourName;
    private String destination;

    /** Travel agent assigned to the tour */
    private String agentId;
    private String agentName;
    private String agentEmail;

    /** Customer who made the booking */
    private String userId;

    private int adults;
    private int children;

    /** Revenue amount in USD (numeric, no $ sign) */
    private long revenueAmount;

    /** Current tour rating snapshot */
    private Double tourRating;

    /** Current tour review count snapshot */
    private Integer tourReviewCount;

    /** Tour start date as ISO string (yyyy-MM-dd) */
    private String bookingDate;

    private String duration;

    private Instant timestamp;
}

