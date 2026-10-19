package com.epam.edp.demo.model;

import com.epam.edp.demo.enums.FeedbackStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "feedbacks")
@Getter
@Setter
@NoArgsConstructor
public class Feedback {

    @Id
    private String id;

    /** Rating from 1 to 5 — always required. */
    private int rating;

    /** Text comment — mandatory when rating <= 3, optional otherwise. */
    private String comment;

    /** Moderation status: PENDING → APPROVED or FLAGGED. */
    private FeedbackStatus status;

    @Indexed
    private String customerId;

    @Indexed
    private String bookingId;

    @Indexed
    private String tourId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
