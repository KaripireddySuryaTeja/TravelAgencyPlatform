package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.BookedTourListResponseDTO;
import com.epam.edp.demo.dto.BookingEvent;
import com.epam.edp.demo.dto.ConfirmBookingChangesRequestDTO;
import com.epam.edp.demo.dto.CreateBookingRequestDTO;
import com.epam.edp.demo.dto.CreateBookingResponseDTO;
import com.epam.edp.demo.dto.PersonalDetailDTO;
import com.epam.edp.demo.dto.UpdateBookingRequestDTO;
import com.epam.edp.demo.enums.BookingState;
import com.epam.edp.demo.enums.DocumentLifecycleStatus;
import com.epam.edp.demo.model.Booking;
import com.epam.edp.demo.model.Tour;
import com.epam.edp.demo.model.User;
import com.epam.edp.demo.repository.BookingDocumentRepository;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.repository.TourRepository;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.util.MealPlanFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final String BOOKING_NOT_FOUND = "Booking not found: ";
    private static final String ADULT_LABEL       = " adult";
    private static final String PRICE_ON_REQUEST  = "Price on request";

    private final BookingRepository bookingRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final BookingDocumentRepository bookingDocumentRepository;
    private final DocumentRetentionCleanupService documentRetentionCleanupService;
    private final ConcurrentMap<String, ReentrantLock> tourBookingLocks = new ConcurrentHashMap<>();

    /** Optional — only wired when RabbitMQ is enabled (app.rabbitmq.enabled=true). */
    private BookingEventPublisher bookingEventPublisher;

    public BookingService(BookingRepository bookingRepository,
                          TourRepository tourRepository,
                          UserRepository userRepository,
                          BookingDocumentRepository bookingDocumentRepository,
                          DocumentRetentionCleanupService documentRetentionCleanupService) {
        this.bookingRepository = bookingRepository;
        this.tourRepository = tourRepository;
        this.userRepository = userRepository;
        this.bookingDocumentRepository = bookingDocumentRepository;
        this.documentRetentionCleanupService = documentRetentionCleanupService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setBookingEventPublisher(BookingEventPublisher bookingEventPublisher) {
        this.bookingEventPublisher = bookingEventPublisher;
    }

    // ─────────────────────────────────────────────
    // Create booking (US6)
    // ─────────────────────────────────────────────
    public CreateBookingResponseDTO createBooking(CreateBookingRequestDTO req, String authenticatedUserId) {

        ReentrantLock tourLock = tourBookingLocks.computeIfAbsent(req.getTourId(), ignored -> new ReentrantLock(true));
        tourLock.lock();
        try {

        // Verify the authenticated user matches the requested userId
        if (!authenticatedUserId.equals(req.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only create bookings for yourself");
        }

        // Validate personalDetails count matches total guests
        int totalGuests = req.getGuests().getAdult() + req.getGuests().getChildren();
        if (req.getPersonalDetails() != null && req.getPersonalDetails().size() != totalGuests) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "personalDetails count (" + req.getPersonalDetails().size()
                            + ") must match total guests (" + totalGuests + ")");
        }

        // Load tour
        Tour tour = tourRepository.findById(req.getTourId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour not found: " + req.getTourId()));

        // Load user to confirm existence
        userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + req.getUserId()));

        // Calculate total price
        String totalPrice = calculateTotalPrice(tour, req.getDuration(), req.getMealPlan(),
                req.getGuests().getAdult());

        // Build Booking entity
        Booking booking = new Booking();
        booking.setUserId(req.getUserId());
        booking.setTourId(req.getTourId());
        booking.setDate(req.getDate());
        booking.setDuration(req.getDuration());
        booking.setMealPlan(req.getMealPlan());
        booking.setAdults(req.getGuests().getAdult());
        booking.setChildren(req.getGuests().getChildren());
        booking.setState(BookingState.BOOKED);
        booking.setTotalPrice(totalPrice);
        booking.setDocumentCount(0);
        booking.setFreeCancellationDaysBefore(tour.getFreeCancellationDaysBefore());

        // Store tour snapshot so data survives if tour is deleted/re-seeded
        booking.setTourName(tour.getName());
        booking.setDestination(tour.getDestination());
        if (tour.getImageUrls() != null && !tour.getImageUrls().isEmpty()) {
            booking.setTourImageUrl(tour.getImageUrls().get(0));
        }

        // Map personal details
        if (req.getPersonalDetails() != null) {
            booking.setPersonalDetails(mapPersonalDetails(req.getPersonalDetails()));
        }

        long updated = tourRepository.incrementBookedCountIfCapacityAvailable(tour.getId());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This tour is fully booked");
        }

        try {
            bookingRepository.save(booking);
        } catch (RuntimeException ex) {
            // Best-effort rollback of reserved seat if booking persistence fails.
            tourRepository.decrementBookedCountIfPositive(tour.getId());
            throw ex;
        }

        // Free cancellation deadline
        LocalDate freeCancelation = null;
        if (tour.getFreeCancellationDaysBefore() != null) {
            freeCancelation = req.getDate().minusDays(tour.getFreeCancellationDaysBefore());
        }

        // Build confirmation message
        String details = buildConfirmationMessage(tour, req);

        // Publish event to RabbitMQ (non-blocking)
        publishEvent("BOOKING_CREATED", booking, tour);

        return CreateBookingResponseDTO.builder()
                .freeCancelation(freeCancelation)
                .details(details)
                .build();
        } finally {
            tourLock.unlock();
        }
    }

    // ─────────────────────────────────────────────
    // Get bookings for a user (US6)
    // ─────────────────────────────────────────────
    public BookedTourListResponseDTO getBookingsForUser(String userId, String authenticatedUserId) {

        if (!authenticatedUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only view your own bookings");
        }

        List<Booking> bookings = bookingRepository.findByUserId(userId);

        List<BookedTourListResponseDTO.BookingItem> items = bookings.stream()
                .map(b -> mapToBookingItem(b, null))
                .collect(Collectors.toList());

        return BookedTourListResponseDTO.builder()
                .bookings(items)
                .build();
    }

    // ─────────────────────────────────────────────
    // Get bookings for an agent (US8)
    // ─────────────────────────────────────────────
    public BookedTourListResponseDTO getBookingsForAgent(String agentId, String authenticatedUserId) {

        // Verify the authenticated user is the agent themselves
        if (!authenticatedUserId.equals(agentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only view bookings for your own tours");
        }

        // Find all tours assigned to this agent
        List<Tour> agentTours = tourRepository.findByAssignedAgentId(agentId);
        if (agentTours.isEmpty()) {
            return BookedTourListResponseDTO.builder().bookings(List.of()).build();
        }

        List<String> tourIds = agentTours.stream()
                .map(Tour::getId)
                .collect(Collectors.toList());

        // Find all bookings for those tours
        List<Booking> bookings = bookingRepository.findByTourIdIn(tourIds);

        List<BookedTourListResponseDTO.BookingItem> items = bookings.stream()
                .map(b -> {
                    // Look up the customer who made the booking
                    BookedTourListResponseDTO.CustomerDetailsDTO customerDetails = buildCustomerDetailsDTO(b.getUserId());
                    return mapToBookingItem(b, customerDetails);
                })
                .collect(Collectors.toList());

        return BookedTourListResponseDTO.builder()
                .bookings(items)
                .build();
    }

    // ─────────────────────────────────────────────
    // Cancel booking (US6 + US8)
    // ─────────────────────────────────────────────
    public LocalDate cancelBooking(String bookingId, String authenticatedUserId, String cancelReason) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        BOOKING_NOT_FOUND + bookingId));

        // Allow the booking owner OR the assigned travel agent to cancel
        if (!booking.getUserId().equals(authenticatedUserId)
                && !isAssignedAgent(booking.getTourId(), authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only cancel your own bookings or bookings for tours assigned to you");
        }

        if (booking.getState() == BookingState.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking is already cancelled");
        }
        if (booking.getState() == BookingState.FINISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel a finished booking");
        }

        // Determine free cancellation deadline
        Tour tour = tourRepository.findById(booking.getTourId()).orElse(null);
        LocalDate freeCancelDeadline = null;
        if (tour != null && tour.getFreeCancellationDaysBefore() != null) {
            freeCancelDeadline = booking.getDate().minusDays(tour.getFreeCancellationDaysBefore());
        }

        booking.setState(BookingState.CANCELLED);
        booking.setCanceledBy(authenticatedUserId);
        booking.setCancelReason(cancelReason);
        bookingRepository.save(booking);

        // Cleanup only passport documents. Payment documents remain for audit/refund compliance.
        documentRetentionCleanupService.cleanupPassportDocumentsForCancelledBooking(
            bookingId,
            "BOOKING_CANCEL_API");

        // Decrement tour's bookedCount
        if (tour != null) {
            tourRepository.decrementBookedCountIfPositive(tour.getId());
        }

        // Publish cancellation event (non-blocking)
        publishEvent("BOOKING_CANCELLED", booking, tour);

        return freeCancelDeadline;
    }

    // ─────────────────────────────────────────────
    // Update booking (Edit)
    // ─────────────────────────────────────────────
    public Map<String, Object> updateBooking(
            String bookingId, String authenticatedUserId, UpdateBookingRequestDTO req) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        BOOKING_NOT_FOUND + bookingId));

        validateBookingForEdit(booking, authenticatedUserId);

        List<String> changes = new ArrayList<>();
        Tour tour = tourRepository.findById(booking.getTourId()).orElse(null);

        applyGuestUpdate(booking, req, changes);
        applyPersonalDetailsIfUnchanged(booking, req);
        applyMealPlanUpdate(booking, req, changes);
        applyDateUpdate(booking, req, changes);
        applyDurationUpdate(booking, req, changes);

        if (tour != null) {
            String newPrice = calculateTotalPrice(
                    tour, booking.getDuration(), booking.getMealPlan(), booking.getAdults());
            booking.setTotalPrice(newPrice);
        }

        bookingRepository.save(booking);

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("message", "Booking updated successfully");
        response.put("changes", changes);
        response.put("bookingId", bookingId);
        response.put("newTotalPrice", booking.getTotalPrice());
        return response;
    }

    public Map<String, Object> confirmBookingChanges(String bookingId,
                                                     String authenticatedUserId,
                                                     ConfirmBookingChangesRequestDTO req) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        BOOKING_NOT_FOUND + bookingId));

        if (!booking.getUserId().equals(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only confirm changes for your own bookings");
        }

        int deletedDocuments = documentRetentionCleanupService.cleanupDocumentsForRemovedGuests(
                bookingId,
                req != null ? req.getRemovedGuestIds() : null,
                "BOOKING_EDIT_CONFIRM");

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("message", "Booking changes confirmed successfully");
        response.put("bookingId", bookingId);
        response.put("deletedDocumentCount", deletedDocuments);
        return response;
    }

    // ─────────────────────────────────────────────
    // Verify documents (US8 - AC3)
    // Travel Agent marks documents as verified
    // ─────────────────────────────────────────────
    public Map<String, Object> verifyDocuments(String bookingId, String authenticatedUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        BOOKING_NOT_FOUND + bookingId));

        // Only the assigned travel agent can verify documents
        if (!isAssignedAgent(booking.getTourId(), authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the assigned travel agent can verify documents");
        }

        if (booking.getState() != BookingState.BOOKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Documents can only be verified for bookings in BOOKED state");
        }

        booking.setState(BookingState.DOCUMENTS_VERIFIED);
        bookingRepository.save(booking);

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("message", "Documents verified successfully");
        response.put("bookingId", bookingId);
        response.put("newState", BookingState.DOCUMENTS_VERIFIED.name());
        return response;
    }

    // ─────────────────────────────────────────────
    // Confirm booking (US8 - AC3/AC4)
    // Travel Agent confirms after document check
    // ─────────────────────────────────────────────
    public Map<String, Object> confirmBooking(String bookingId, String authenticatedUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        BOOKING_NOT_FOUND + bookingId));

        // Only the assigned travel agent can confirm
        if (!isAssignedAgent(booking.getTourId(), authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the assigned travel agent can confirm bookings");
        }

        if (booking.getState() != BookingState.BOOKED
                && booking.getState() != BookingState.DOCUMENTS_VERIFIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Booking can only be confirmed from BOOKED or DOCUMENTS_VERIFIED state");
        }

        booking.setState(BookingState.CONFIRMED);
        bookingRepository.save(booking);

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("message", "Booking confirmed successfully");
        response.put("bookingId", bookingId);
        response.put("newState", BookingState.CONFIRMED.name());
        return response;
    }

    // ─────────────────────────────────────────────
    // Helper: check if user is the assigned agent for a tour
    // ─────────────────────────────────────────────
    private boolean isAssignedAgent(String tourId, String userId) {
        if (tourId == null || userId == null) {
            return false;
        }
        Tour tour = tourRepository.findById(tourId).orElse(null);
        return tour != null && userId.equals(tour.getAssignedAgentId());
    }

    private void validateBookingForEdit(Booking booking, String authenticatedUserId) {
        // Allow the booking owner OR the assigned travel agent to edit
        if (!booking.getUserId().equals(authenticatedUserId)
                && !isAssignedAgent(booking.getTourId(), authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only edit your own bookings or bookings for tours assigned to you");
        }
        if (booking.getState() == BookingState.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot edit a cancelled booking");
        }
        if (booking.getState() == BookingState.FINISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot edit a finished booking");
        }
        if (booking.getState() == BookingState.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot edit a confirmed booking. Only cancellation is allowed.");
        }
        if (booking.getState() == BookingState.STARTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot edit a booking that has already started.");
        }
    }

    private void applyGuestUpdate(Booking booking, UpdateBookingRequestDTO req, List<String> changes) {
        if (req.getGuests() == null) {
            return;
        }
        int newAdults = req.getGuests().getAdult();
        int newChildren = req.getGuests().getChildren();
        if (booking.getAdults() == newAdults && booking.getChildren() == newChildren) {
            return;
        }
        String oldGuestsStr = formatGuests(booking);
        booking.setAdults(newAdults);
        booking.setChildren(newChildren);
        if (req.getPersonalDetails() != null) {
            booking.setPersonalDetails(mapPersonalDetails(req.getPersonalDetails()));
        }
        changes.add("Number of tourists: " + oldGuestsStr + " → " + formatGuests(booking));
    }

    private void applyPersonalDetailsIfUnchanged(Booking booking, UpdateBookingRequestDTO req) {
        if (req.getPersonalDetails() == null) {
            return;
        }
        boolean guestsUnchanged = req.getGuests() == null
                || (booking.getAdults() == req.getGuests().getAdult()
                    && booking.getChildren() == req.getGuests().getChildren());
        if (guestsUnchanged) {
            booking.setPersonalDetails(mapPersonalDetails(req.getPersonalDetails()));
        }
    }

    private void applyMealPlanUpdate(Booking booking, UpdateBookingRequestDTO req, List<String> changes) {
        if (req.getMealPlan() == null || req.getMealPlan().equals(booking.getMealPlan())) {
            return;
        }
        String oldMeal = MealPlanFormatter.format(booking.getMealPlan());
        booking.setMealPlan(req.getMealPlan());
        changes.add("Meal plan: " + oldMeal + " → " + MealPlanFormatter.format(req.getMealPlan()));
    }

    private void applyDateUpdate(Booking booking, UpdateBookingRequestDTO req, List<String> changes) {
        if (req.getDate() != null && !req.getDate().equals(booking.getDate())) {
            booking.setDate(req.getDate());
            changes.add("Start date updated");
        }
    }

    private void applyDurationUpdate(Booking booking, UpdateBookingRequestDTO req, List<String> changes) {
        if (req.getDuration() == null || req.getDuration().equals(booking.getDuration())) {
            return;
        }
        String oldDur = booking.getDuration();
        booking.setDuration(req.getDuration());
        changes.add("Duration: " + oldDur + " → " + req.getDuration());
    }


    // ─────────────────────────────────────────────
    // Scheduled: auto-finish tours past their end date
    // Runs every day at midnight
    // ─────────────────────────────────────────────
    @Scheduled(cron = "0 0 0 * * *")
    public void markStartedAndFinishedBookings() {
        LocalDate today = LocalDate.now();

        // 1. Transition CONFIRMED → STARTED when tour start date arrives
        List<Booking> confirmedBookings = bookingRepository.findByState(BookingState.CONFIRMED);
        List<Booking> toStart = new ArrayList<>();
        for (Booking b : confirmedBookings) {
            if (b.getDate() != null && !today.isBefore(b.getDate())) {
                int days = parseDurationDays(b.getDuration());
                LocalDate endDate = b.getDate().plusDays(days);
                if (today.isBefore(endDate)) {
                    b.setState(BookingState.STARTED);
                    toStart.add(b);
                }
            }
        }
        if (!toStart.isEmpty()) {
            bookingRepository.saveAll(toStart);
            toStart.forEach(b -> {
                Tour t = tourRepository.findById(b.getTourId()).orElse(null);
                publishEvent("BOOKING_STARTED", b, t);
            });
        }

        // 2. Transition active bookings → FINISHED when end date passes
        List<Booking> activeBookings = new ArrayList<>();
        activeBookings.addAll(bookingRepository.findByState(BookingState.BOOKED));
        activeBookings.addAll(bookingRepository.findByState(BookingState.DOCUMENTS_VERIFIED));
        activeBookings.addAll(bookingRepository.findByState(BookingState.CONFIRMED));
        activeBookings.addAll(bookingRepository.findByState(BookingState.STARTED));
        List<Booking> toFinish = new ArrayList<>();
        for (Booking b : activeBookings) {
            if (b.getDate() != null && b.getDuration() != null) {
                int days = parseDurationDays(b.getDuration());
                LocalDate endDate = b.getDate().plusDays(days);
                if (!today.isBefore(endDate)) {
                    b.setState(BookingState.FINISHED);
                    toFinish.add(b);
                }
            }
        }

        if (!toFinish.isEmpty()) {
            bookingRepository.saveAll(toFinish);
            toFinish.forEach(b -> {
                Tour t = tourRepository.findById(b.getTourId()).orElse(null);
                publishEvent("BOOKING_FINISHED", b, t);
            });
        }
    }

    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    private BookedTourListResponseDTO.BookingItem mapToBookingItem(
            Booking booking, BookedTourListResponseDTO.CustomerDetailsDTO customerDetails) {
        Tour tour = tourRepository.findById(booking.getTourId()).orElse(null);

        String tourName     = resolveTourName(booking, tour);
        String destination  = resolveDestination(booking, tour);
        String tourImageUrl = resolveTourImageUrl(booking, tour);
        double rating       = (tour != null && tour.getRating() != null) ? tour.getRating() : 0.0;

        long docCount = bookingDocumentRepository.countByBookingIdAndLifecycleStatus(
                booking.getId(), DocumentLifecycleStatus.ACTIVE);

        return BookedTourListResponseDTO.BookingItem.builder()
                .id(booking.getId())
                .tourId(booking.getTourId())
                .state(booking.getState() != null ? booking.getState().name() : null)
                .tourImageUrl(tourImageUrl)
                .name(tourName)
                .destination(destination)
                .rating(rating)
                .tourDetails(buildTourDetailsDTO(booking))
                .travelAgent(buildTravelAgentDTO(tour))
                .canceledBy(booking.getCanceledBy())
                .cancelReason(booking.getCancelReason())
                .rawDate(booking.getDate() != null ? booking.getDate().toString() : null)
                .rawDuration(booking.getDuration())
                .rawMealPlan(booking.getMealPlan())
                .rawAdults(booking.getAdults())
                .rawChildren(booking.getChildren())
                .freeCancellationDate(resolveFreeCancellationDate(booking, tour))
                .personalDetails(mapPersonalDetailItems(booking))
                .customerDetails(customerDetails)
                .documentCount(docCount)
                .build();
    }

    private BookedTourListResponseDTO.CustomerDetailsDTO buildCustomerDetailsDTO(String userId) {
        if (userId == null) return null;
        User customer = userRepository.findById(userId).orElse(null);
        if (customer == null) return null;
        String name = (customer.getFirstName() + " " + customer.getLastName()).trim();
        return BookedTourListResponseDTO.CustomerDetailsDTO.builder()
                .name(name)
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .build();
    }

    private String resolveTourName(Booking booking, Tour tour) {
        return (tour != null) ? tour.getName() : booking.getTourName();
    }

    private String resolveDestination(Booking booking, Tour tour) {
        return (tour != null) ? tour.getDestination() : booking.getDestination();
    }

    private String resolveTourImageUrl(Booking booking, Tour tour) {
        if (tour != null && tour.getImageUrls() != null && !tour.getImageUrls().isEmpty()) {
            return tour.getImageUrls().get(0);
        }
        return booking.getTourImageUrl();
    }

    private String resolveFreeCancellationDate(Booking booking, Tour tour) {
        if (booking.getDate() == null) {
            return null;
        }
        Integer daysBefore = resolveDaysBefore(booking, tour);
        if (daysBefore == null) {
            return null;
        }
        return booking.getDate().minusDays(daysBefore).toString();
    }

    private Integer resolveDaysBefore(Booking booking, Tour tour) {
        if (tour != null && tour.getFreeCancellationDaysBefore() != null) {
            return tour.getFreeCancellationDaysBefore();
        }
        return booking.getFreeCancellationDaysBefore();
    }

    private List<BookedTourListResponseDTO.PersonalDetailItem> mapPersonalDetailItems(Booking booking) {
        if (booking.getPersonalDetails() == null) {
            return null;
        }
        return booking.getPersonalDetails().stream()
                .map(pd -> BookedTourListResponseDTO.PersonalDetailItem.builder()
                        .firstName(pd.getFirstName())
                        .lastName(pd.getLastName())
                        .build())
                .collect(Collectors.toList());
    }

    private BookedTourListResponseDTO.TourDetailsDTO buildTourDetailsDTO(Booking booking) {
        // Format date: "Jan 17, 2025 (7 days)"
        String dateStr = null;
        if (booking.getDate() != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
            dateStr = booking.getDate().format(fmt) + " (" + booking.getDuration() + ")";
        }

        // Format meal plan
        String mealPlanFormatted = MealPlanFormatter.format(booking.getMealPlan());

        // Format guests: "Jhonson Doe (1 adult)" or "Jhonson Doe (2 adults, 1 child)"
        String guestsStr = formatGuests(booking);

        // Documents: "0 items"
        String documents = booking.getDocumentCount() + " item" + (booking.getDocumentCount() == 1 ? "" : "s");

        return BookedTourListResponseDTO.TourDetailsDTO.builder()
                .date(dateStr)
                .mealPlan(mealPlanFormatted)
                .guests(guestsStr)
                .totalPrice(booking.getTotalPrice())
                .documents(documents)
                .build();
    }

    private BookedTourListResponseDTO.TravelAgentDTO buildTravelAgentDTO(Tour tour) {
        if (tour == null || tour.getAssignedAgentId() == null) {
            return null;
        }
        User agent = userRepository.findById(tour.getAssignedAgentId()).orElse(null);
        if (agent == null) {
            return null;
        }
        String agentName = (agent.getFirstName() + " " + agent.getLastName()).trim();
        return BookedTourListResponseDTO.TravelAgentDTO.builder()
                .name(agentName)
                .email(agent.getEmail())
                .phone(agent.getPhone())
                .messenger(agent.getMessengerLink())
                .build();
    }

    private List<Booking.PersonalDetail> mapPersonalDetails(List<PersonalDetailDTO> dtos) {
        return dtos.stream()
                .map(pd -> {
                    Booking.PersonalDetail d = new Booking.PersonalDetail();
                    d.setFirstName(pd.getFirstName());
                    d.setLastName(pd.getLastName());
                    return d;
                })
                .collect(Collectors.toList());
    }

    private String formatGuests(Booking booking) {
        String leadName = "";
        if (booking.getPersonalDetails() != null && !booking.getPersonalDetails().isEmpty()) {
            Booking.PersonalDetail lead = booking.getPersonalDetails().get(0);
            leadName = (lead.getFirstName() + " " + lead.getLastName()).trim() + " ";
        }
        int adults = booking.getAdults();
        int children = booking.getChildren();
        StringBuilder sb = new StringBuilder(leadName);
        sb.append("(");
        sb.append(adults).append(ADULT_LABEL).append(adults != 1 ? "s" : "");
        if (children > 0) {
            sb.append(", ").append(children).append(" child").append(children != 1 ? "ren" : "");
        }
        sb.append(")");
        return sb.toString();
    }

    private String calculateTotalPrice(Tour tour, String duration, String mealPlan, int adults) {
        if (tour.getPricePerDuration() == null) {
            return PRICE_ON_REQUEST;
        }

        String basePriceStr = tour.getPricePerDuration().get(duration);
        if (basePriceStr == null) {
            return PRICE_ON_REQUEST;
        }

        try {
            long basePrice = parsePriceLong(basePriceStr);
            long supplement = 0;
            if (tour.getMealSupplementsPerDay() != null) {
                String suppStr = tour.getMealSupplementsPerDay().get(mealPlan);
                if (suppStr != null) {
                    supplement = parsePriceLong(suppStr);
                }
            }
            int days = parseDurationDays(duration);
            long totalPerPerson = basePrice + (supplement * days);
            long total = totalPerPerson * Math.max(adults, 1);
            return "$" + total;
        } catch (NumberFormatException e) {
            return basePriceStr;
        }
    }

    private long parsePriceLong(String price) {
        // Strip everything except digits
        String cleaned = price.replaceAll("[^0-9]", "");
        return Long.parseLong(cleaned);
    }

    private int parseDurationDays(String duration) {
        // e.g. "7 days" → 7, "10 days" → 10
        if (duration == null) {
            return 0;
        }
        String[] parts = duration.trim().split("\\s+");
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ─────────────────────────────────────────────
    // RabbitMQ event helper
    // ─────────────────────────────────────────────

    private void publishEvent(String eventType, Booking booking, Tour tour) {
        if (bookingEventPublisher == null) return;

        // Resolve agent info from tour
        String agentId = null, agentName = null, agentEmail = null;
        if (tour != null && tour.getAssignedAgentId() != null) {
            agentId = tour.getAssignedAgentId();
            User agent = userRepository.findById(agentId).orElse(null);
            if (agent != null) {
                agentName = (agent.getFirstName() + " " + agent.getLastName()).trim();
                agentEmail = agent.getEmail();
            }
        }

        // Parse revenue from totalPrice (e.g. "$2000" → 2000)
        long revenue = 0;
        if (booking.getTotalPrice() != null) {
            try {
                revenue = Long.parseLong(booking.getTotalPrice().replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {
                revenue = 1000L * Math.max(booking.getAdults(), 1); // fallback mock
            }
        } else {
            revenue = 1000L * Math.max(booking.getAdults(), 1); // mock: $1000 per person
        }

        BookingEvent event = BookingEvent.builder()
                .eventType(eventType)
                .bookingId(booking.getId())
                .tourId(booking.getTourId())
                .tourName(tour != null ? tour.getName() : booking.getTourName())
                .destination(tour != null ? tour.getDestination() : booking.getDestination())
                .agentId(agentId)
                .agentName(agentName)
                .agentEmail(agentEmail)
                .userId(booking.getUserId())
                .adults(booking.getAdults())
                .children(booking.getChildren())
                .revenueAmount(revenue)
                .tourRating(tour != null ? tour.getRating() : null)
                .tourReviewCount(tour != null ? tour.getReviewCount() : null)
                .bookingDate(booking.getDate() != null ? booking.getDate().toString() : null)
                .duration(booking.getDuration())
                .build();

        bookingEventPublisher.publish(event);
    }

    private String buildConfirmationMessage(Tour tour, CreateBookingRequestDTO req) {
        String hotelOrDest = (tour.getHotelName() != null && !tour.getHotelName().isBlank())
                ? tour.getHotelName()
                : tour.getDestination();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
        String formattedDate = req.getDate().format(fmt);
        String mealPlanFormatted = MealPlanFormatter.format(req.getMealPlan());
        int adults = req.getGuests().getAdult();
        String adultStr = adults + ADULT_LABEL + (adults != 1 ? "s" : "");

        return "You have booked at " + hotelOrDest
                + ", starting date " + formattedDate
                + " (" + req.getDuration() + "), "
                + mealPlanFormatted
                + " for " + adultStr
                + " successfully. Please upload your travel documents to the booking on the"
                + " 'My Tours' page and wait for the Travel Agent to contact you.";
    }
}

