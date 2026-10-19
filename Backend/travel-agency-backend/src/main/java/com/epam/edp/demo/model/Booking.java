package com.epam.edp.demo.model;

import com.epam.edp.demo.enums.BookingState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String tourId;

    private LocalDate date;
    private String duration;
    private String mealPlan;
    private int adults;
    private int children;
    private List<PersonalDetail> personalDetails;
    private BookingState state;
    private String totalPrice;
    private int documentCount = 0;
    private Integer freeCancellationDaysBefore;
    private String tourName;
    private String destination;
    private String tourImageUrl;
    private String canceledBy;
    private String cancelReason;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PersonalDetail {
        private String firstName;
        private String lastName;
    }
}

