package com.epam.edp.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BookedTourListResponse", description = "List of bookings for the authenticated user with detailed tour and status information")
public class BookedTourListResponseDTO {

    @Schema(description = "Array of user bookings with current status and tour details")
    private List<BookingItem> bookings;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BookingItem", description = "Booking details including tour information, status, and assigned travel agent")
    public static class BookingItem {
        @Schema(description = "Booking identifier", example = "6641b2ea5c8f8d4f1ab67890")
        private String id;
        @Schema(description = "Associated tour identifier", example = "6641b2ea5c8f8d4f1ab67891")
        private String tourId;
        @Schema(description = "Current booking status (CONFIRMED, CANCELLED, PENDING_PAYMENT)", example = "CONFIRMED")
        private String state;
        @Schema(description = "Tour thumbnail image URL", example = "https://cdn.example.com/tours/bali-thumbnail.jpg")
        private String tourImageUrl;
        @Schema(description = "Tour name", example = "Bali Explorer")
        private String name;
        @Schema(description = "Tour destination", example = "Bali")
        private String destination;
        @Schema(description = "Tour average rating", example = "4.7")
        private double rating;
        @Schema(description = "Booking travel details (date, meal plan, guests, pricing)")
        private TourDetailsDTO tourDetails;
        @Schema(description = "Assigned travel agent contact information")
        private TravelAgentDTO travelAgent;
        @Schema(description = "Username who canceled the booking, if applicable")
        private String canceledBy;
        @Schema(description = "Reason provided for booking cancellation")
        private String cancelReason;
        @Schema(description = "Raw travel date in ISO-8601 format for editing", example = "2026-08-15")
        private String rawDate;
        @Schema(description = "Raw duration selection for editing", example = "7 days / 6 nights")
        private String rawDuration;
        @Schema(description = "Raw meal plan selection for editing", example = "ALL_INCLUSIVE")
        private String rawMealPlan;
        @Schema(description = "Raw adult count for editing", example = "2")
        private int rawAdults;
        @Schema(description = "Raw children count for editing", example = "1")
        private int rawChildren;
        @Schema(description = "Last date for free cancellation (ISO-8601)", example = "2026-08-10")
        private String freeCancellationDate;
        @Schema(description = "List of travelers in this booking")
        private java.util.List<PersonalDetailItem> personalDetails;
        // Agent view: customer who made the booking
        private CustomerDetailsDTO customerDetails;
        @Schema(description = "Number of active uploaded documents for this booking")
        private long documentCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDetailsDTO {
        private String name;
        private String email;
        private String phone;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "PersonalDetail", description = "Traveler identity information in a booking")
    public static class PersonalDetailItem {
        @Schema(description = "Traveler first name", example = "Alex")
        private String firstName;
        @Schema(description = "Traveler last name", example = "Johnson")
        private String lastName;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "BookingTourDetails", description = "Booking-specific tour configuration and pricing details")
    public static class TourDetailsDTO {
        @Schema(description = "Travel start date (formatted string)", example = "August 15, 2026")
        private String date;
        @Schema(description = "Selected meal plan", example = "All Inclusive")
        private String mealPlan;
        @Schema(description = "Guest summary (adults and children)", example = "2 Adults, 1 Child")
        private String guests;
        @Schema(description = "Total booking price", example = "$4,499.00")
        private String totalPrice;
        @Schema(description = "Booking confirmation documents or voucher reference")
        private String documents;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "TravelAgent", description = "Contact information for the assigned travel agent")
    public static class TravelAgentDTO {
        @Schema(description = "Travel agent full name", example = "Raj Patel")
        private String name;
        @Schema(description = "Travel agent email", example = "raj.patel@travelagency.com")
        private String email;
        @Schema(description = "Travel agent phone number", example = "+91-9876-543210")
        private String phone;
        @Schema(description = "Travel agent messaging platform handle (WhatsApp, Telegram, etc.)", example = "raj_patel_ta")
        private String messenger;
    }
}

