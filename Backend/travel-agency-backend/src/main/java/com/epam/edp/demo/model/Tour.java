package com.epam.edp.demo.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "tours")
@Getter
@Setter
@NoArgsConstructor
public class Tour {

    @Id
    private String id;
    private String name;
    private String destination;
    private String summary;
    private List<String> imageUrls;
    private Double rating;
    private Integer reviewCount;
    private List<LocalDate> startDates;
    private List<String> durations;
    private Map<String, String> pricePerDuration;
    private List<String> mealPlans;
    private Map<String, String> mealSupplementsPerDay;
    private String tourType;
    private String hotelName;
    private String hotelDescription;
    private String accommodation;
    private Map<String, String> customDetails;
    private GuestQuantity guestQuantity;
    private Integer totalCapacity;
    private Integer bookedCount;
    private Integer freeCancellationDaysBefore;
    private String assignedAgentId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class GuestQuantity {
        private Integer adultsMaxValue;
        private Integer childrenMaxValue;
        private Integer totalMaxValue;
    }
}