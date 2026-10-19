package com.epam.edp.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {

    private String id;
    private int rating;
    private String comment;
    private String status;
    private String customerId;
    private String bookingId;
    private String tourId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
