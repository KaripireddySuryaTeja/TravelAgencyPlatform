package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.BookedTourListResponseDTO;
import com.epam.edp.demo.dto.BookingGuestsDTO;
import com.epam.edp.demo.dto.ConfirmBookingChangesRequestDTO;
import com.epam.edp.demo.dto.CreateBookingRequestDTO;
import com.epam.edp.demo.dto.CreateBookingResponseDTO;
import com.epam.edp.demo.dto.PersonalDetailDTO;
import com.epam.edp.demo.enums.BookingState;
import com.epam.edp.demo.model.Booking;
import com.epam.edp.demo.model.Tour;
import com.epam.edp.demo.model.User;
import com.epam.edp.demo.repository.BookingDocumentRepository;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.repository.TourRepository;
import com.epam.edp.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingDocumentRepository bookingDocumentRepository;

    @Mock
    private DocumentRetentionCleanupService documentRetentionCleanupService;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, tourRepository, userRepository,
                bookingDocumentRepository, documentRetentionCleanupService);
    }

    @Test
    void createBooking_forbiddenWhenAuthenticatedUserDoesNotMatchRequestUser() {
        CreateBookingRequestDTO req = request("user-2", "tour-1", 2, "BB");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.createBooking(req, "user-1"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(tourRepository, never()).findById(any());
    }

    @Test
    void createBooking_notFoundWhenTourDoesNotExist() {
        CreateBookingRequestDTO req = request("user-1", "tour-404", 2, "BB");
        when(tourRepository.findById("tour-404")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.createBooking(req, "user-1"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createBooking_notFoundWhenUserDoesNotExist() {
        CreateBookingRequestDTO req = request("user-1", "tour-1", 2, "BB");
        Tour tour = baseTour("tour-1");
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.createBooking(req, "user-1"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createBooking_conflictWhenAtomicCapacityCheckFails() {
        CreateBookingRequestDTO req = request("user-1", "tour-1", 2, "BB");
        Tour tour = baseTour("tour-1");
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(new User()));
        when(tourRepository.incrementBookedCountIfCapacityAvailable("tour-1")).thenReturn(0L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.createBooking(req, "user-1"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_successPersistsBookingAndBuildsResponse() {
        CreateBookingRequestDTO req = request("user-1", "tour-1", 2, "BB");
        Tour tour = baseTour("tour-1");
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(new User()));
        when(tourRepository.incrementBookedCountIfCapacityAvailable("tour-1")).thenReturn(1L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateBookingResponseDTO response = bookingService.createBooking(req, "user-1");

        assertNotNull(response);
        assertEquals(LocalDate.of(2026, 6, 3), response.getFreeCancelation());
        assertTrue(response.getDetails().contains("You have booked at"));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_mapsPersonalDetailsIntoBookingEntity() {
        CreateBookingRequestDTO req = request("user-1", "tour-1", 2, "HB");
        Tour tour = baseTour("tour-1");
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(new User()));
        when(tourRepository.incrementBookedCountIfCapacityAvailable("tour-1")).thenReturn(1L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.createBooking(req, "user-1");

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        Booking saved = captor.getValue();
        assertEquals(3, saved.getPersonalDetails().size());
        assertEquals("Person1", saved.getPersonalDetails().get(0).getFirstName());
    }

    @Test
    void createBooking_setsNullFreeCancellationWhenTourConfigMissing() {
        CreateBookingRequestDTO req = request("user-1", "tour-1", 2, "BB");
        Tour tour = baseTour("tour-1");
        tour.setFreeCancellationDaysBefore(null);
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(new User()));
        when(tourRepository.incrementBookedCountIfCapacityAvailable("tour-1")).thenReturn(1L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateBookingResponseDTO response = bookingService.createBooking(req, "user-1");

        assertNull(response.getFreeCancelation());
    }

    @Test
    void createBooking_rollsBackSeatReservationWhenSaveFails() {
        CreateBookingRequestDTO req = request("user-1", "tour-1", 2, "BB");
        Tour tour = baseTour("tour-1");
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(new User()));
        when(tourRepository.incrementBookedCountIfCapacityAvailable("tour-1")).thenReturn(1L);
        when(bookingRepository.save(any(Booking.class))).thenThrow(new RuntimeException("db write fail"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(req, "user-1"));

        assertEquals("db write fail", ex.getMessage());
        verify(tourRepository).decrementBookedCountIfPositive("tour-1");
    }

    @Test
    void createBooking_usesPriceOnRequestWhenDurationPriceMissing() {
        CreateBookingRequestDTO req = request("user-1", "tour-1", 2, "BB");
        Tour tour = baseTour("tour-1");
        tour.setPricePerDuration(Map.of("3 days", "$300"));
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(new User()));
        when(tourRepository.incrementBookedCountIfCapacityAvailable("tour-1")).thenReturn(1L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.createBooking(req, "user-1");

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertEquals("Price on request", captor.getValue().getTotalPrice());
    }

    @Test
    void createBooking_calculatesSupplementedTotalPriceForAdults() {
        CreateBookingRequestDTO req = request("user-1", "tour-1", 2, "HB");
        Tour tour = baseTour("tour-1");
        tour.setPricePerDuration(Map.of("7 days", "$1000"));
        tour.setMealSupplementsPerDay(Map.of("HB", "$10"));
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(new User()));
        when(tourRepository.incrementBookedCountIfCapacityAvailable("tour-1")).thenReturn(1L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.createBooking(req, "user-1");

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertEquals("$2140", captor.getValue().getTotalPrice());
    }

    @Test
    void createBooking_usesAtLeastOneAdultForPricingWhenAdultCountIsZero() {
        CreateBookingRequestDTO req = request("user-1", "tour-1", 0, "BB");
        Tour tour = baseTour("tour-1");
        tour.setPricePerDuration(Map.of("7 days", "$500"));
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(new User()));
        when(tourRepository.incrementBookedCountIfCapacityAvailable("tour-1")).thenReturn(1L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.createBooking(req, "user-1");

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertEquals("$500", captor.getValue().getTotalPrice());
    }

    @Test
    void getBookingsForUser_forbiddenWhenUserMismatch() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.getBookingsForUser("user-1", "user-2"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(bookingRepository, never()).findByUserId(any());
    }

    @Test
    void getBookingsForUser_returnsMappedBookings() {
        Booking booking = booked("b-1", "user-1", "tour-1", "7 days", "HB", 2, 1);
        Tour tour = baseTour("tour-1");
        tour.setName("Paris City Escape");
        tour.setDestination("Paris");
        tour.setImageUrls(List.of("img-1", "img-2"));
        User agent = new User();
        agent.setFirstName("Sarah");
        agent.setLastName("Connor");
        agent.setEmail("sarah@example.com");
        agent.setPhone("123");
        agent.setMessengerLink("tg://sarah");
        tour.setAssignedAgentId("agent-1");

        when(bookingRepository.findByUserId("user-1")).thenReturn(List.of(booking));
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));
        when(userRepository.findById("agent-1")).thenReturn(Optional.of(agent));

        BookedTourListResponseDTO response = bookingService.getBookingsForUser("user-1", "user-1");

        assertEquals(1, response.getBookings().size());
        BookedTourListResponseDTO.BookingItem item = response.getBookings().get(0);
        assertEquals("b-1", item.getId());
        assertEquals("img-1", item.getTourImageUrl());
        assertEquals("Paris City Escape", item.getName());
        assertEquals("Paris", item.getDestination());
        assertEquals("Sarah Connor", item.getTravelAgent().getName());
        assertTrue(item.getTourDetails().getGuests().contains("adult"));
    }

    @Test
    void getBookingsForUser_mapsGracefullyWhenTourMissing() {
        Booking booking = booked("b-1", "user-1", "tour-404", "7 days", "BB", 1, 0);

        when(bookingRepository.findByUserId("user-1")).thenReturn(List.of(booking));
        when(tourRepository.findById("tour-404")).thenReturn(Optional.empty());

        BookedTourListResponseDTO response = bookingService.getBookingsForUser("user-1", "user-1");

        BookedTourListResponseDTO.BookingItem item = response.getBookings().get(0);
        assertNull(item.getName());
        assertNull(item.getDestination());
        assertNull(item.getTravelAgent());
    }

    @Test
    void cancelBooking_notFoundWhenBookingMissing() {
        when(bookingRepository.findById("b-404")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.cancelBooking("b-404", "user-1", "reason"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void cancelBooking_forbiddenWhenUserIsNotOwner() {
        Booking booking = booked("b-1", "owner-1", "tour-1", "7 days", "BB", 1, 0);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.cancelBooking("b-1", "other-user", "reason"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void cancelBooking_conflictWhenAlreadyCancelled() {
        Booking booking = booked("b-1", "user-1", "tour-1", "7 days", "BB", 1, 0);
        booking.setState(BookingState.CANCELLED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.cancelBooking("b-1", "user-1", "reason"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void cancelBooking_conflictWhenBookingFinished() {
        Booking booking = booked("b-1", "user-1", "tour-1", "7 days", "BB", 1, 0);
        booking.setState(BookingState.FINISHED);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.cancelBooking("b-1", "user-1", "reason"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void cancelBooking_successUpdatesStateReasonAndDecrementsSeats() {
        Booking booking = booked("b-1", "user-1", "tour-1", "7 days", "BB", 1, 0);
        Tour tour = baseTour("tour-1");
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));

        LocalDate deadline = bookingService.cancelBooking("b-1", "user-1", "changed plans");

        assertEquals(LocalDate.of(2026, 6, 3), deadline);
        assertEquals(BookingState.CANCELLED, booking.getState());
        assertEquals("user-1", booking.getCanceledBy());
        assertEquals("changed plans", booking.getCancelReason());
        verify(bookingRepository).save(booking);
        verify(documentRetentionCleanupService)
            .cleanupPassportDocumentsForCancelledBooking("b-1", "BOOKING_CANCEL_API");
        verify(tourRepository).decrementBookedCountIfPositive("tour-1");
    }

    @Test
    void cancelBooking_returnsNullDeadlineWhenTourNotFound() {
        Booking booking = booked("b-1", "user-1", "tour-404", "7 days", "BB", 1, 0);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(tourRepository.findById("tour-404")).thenReturn(Optional.empty());

        LocalDate deadline = bookingService.cancelBooking("b-1", "user-1", "reason");

        assertNull(deadline);
        verify(documentRetentionCleanupService)
            .cleanupPassportDocumentsForCancelledBooking("b-1", "BOOKING_CANCEL_API");
        verify(tourRepository, never()).decrementBookedCountIfPositive(any());
    }

    @Test
    void markStartedAndFinishedBookings_marksPastEndDateBookingsAsFinished() {
        Booking done = booked("b-1", "user-1", "tour-1", "1 days", "BB", 1, 0);
        done.setDate(LocalDate.now().minusDays(3));
        Booking active = booked("b-2", "user-1", "tour-1", "10 days", "BB", 1, 0);
        active.setDate(LocalDate.now());
        when(bookingRepository.findByState(BookingState.CONFIRMED)).thenReturn(List.of());
        when(bookingRepository.findByState(BookingState.BOOKED)).thenReturn(List.of(done, active));
        when(bookingRepository.findByState(BookingState.DOCUMENTS_VERIFIED)).thenReturn(List.of());
        when(bookingRepository.findByState(BookingState.STARTED)).thenReturn(List.of());

        bookingService.markStartedAndFinishedBookings();

        assertEquals(BookingState.FINISHED, done.getState());
        assertEquals(BookingState.BOOKED, active.getState());
        verify(bookingRepository).saveAll(List.of(done));
    }

    @Test
    void markStartedAndFinishedBookings_skipsSaveWhenNothingToFinish() {
        Booking active = booked("b-2", "user-1", "tour-1", "10 days", "BB", 1, 0);
        active.setDate(LocalDate.now());
        when(bookingRepository.findByState(BookingState.CONFIRMED)).thenReturn(List.of());
        when(bookingRepository.findByState(BookingState.BOOKED)).thenReturn(List.of(active));
        when(bookingRepository.findByState(BookingState.DOCUMENTS_VERIFIED)).thenReturn(List.of());
        when(bookingRepository.findByState(BookingState.STARTED)).thenReturn(List.of());

        bookingService.markStartedAndFinishedBookings();

        assertEquals(BookingState.BOOKED, active.getState());
        verify(bookingRepository, never()).saveAll(any());
    }

    @Test
    void markStartedAndFinishedBookings_ignoresBookingsWithInvalidDuration() {
        Booking invalid = booked("b-3", "user-1", "tour-1", "n/a", "BB", 1, 0);
        invalid.setDate(LocalDate.now().minusDays(10));
        when(bookingRepository.findByState(BookingState.CONFIRMED)).thenReturn(List.of());
        when(bookingRepository.findByState(BookingState.BOOKED)).thenReturn(List.of(invalid));
        when(bookingRepository.findByState(BookingState.DOCUMENTS_VERIFIED)).thenReturn(List.of());
        when(bookingRepository.findByState(BookingState.STARTED)).thenReturn(List.of());

        bookingService.markStartedAndFinishedBookings();

        assertEquals(BookingState.FINISHED, invalid.getState());
    }

    @Test
    void createBooking_keepsUnknownMealPlanCodeInConfirmationMessage() {
        CreateBookingRequestDTO req = request("user-1", "tour-1", 1, "CUSTOM_PLAN");
        Tour tour = baseTour("tour-1");
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(new User()));
        when(tourRepository.incrementBookedCountIfCapacityAvailable("tour-1")).thenReturn(1L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateBookingResponseDTO response = bookingService.createBooking(req, "user-1");

        assertTrue(response.getDetails().contains("CUSTOM_PLAN"));
    }

    @Test
    void cancelBooking_returnsNullDeadlineWhenTourCancellationConfigMissing() {
        Booking booking = booked("b-1", "user-1", "tour-1", "7 days", "BB", 1, 0);
        Tour tour = baseTour("tour-1");
        tour.setFreeCancellationDaysBefore(null);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));

        LocalDate deadline = bookingService.cancelBooking("b-1", "user-1", null);

        assertNull(deadline);
        verify(documentRetentionCleanupService)
            .cleanupPassportDocumentsForCancelledBooking("b-1", "BOOKING_CANCEL_API");
        verify(tourRepository).decrementBookedCountIfPositive("tour-1");
    }

    @Test
    void getBookingsForUser_formatsSingularChildLabel() {
        Booking booking = booked("b-1", "user-1", "tour-1", "7 days", "BB", 2, 1);
        Tour tour = baseTour("tour-1");
        tour.setName("Paris City Escape");
        tour.setDestination("Paris");
        when(bookingRepository.findByUserId("user-1")).thenReturn(List.of(booking));
        when(tourRepository.findById("tour-1")).thenReturn(Optional.of(tour));

        BookedTourListResponseDTO response = bookingService.getBookingsForUser("user-1", "user-1");

        assertTrue(response.getBookings().get(0).getTourDetails().getGuests().contains("1 child"));
    }

    @Test
    void confirmBookingChanges_deletesRemovedGuestDocumentsOnlyOnConfirm() {
        Booking booking = booked("b-1", "user-1", "tour-1", "7 days", "BB", 2, 1);
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));

        ConfirmBookingChangesRequestDTO request = new ConfirmBookingChangesRequestDTO();
        request.setRemovedGuestIds(List.of("GUEST_3"));
        when(documentRetentionCleanupService.cleanupDocumentsForRemovedGuests(
                "b-1",
                List.of("GUEST_3"),
                "BOOKING_EDIT_CONFIRM")).thenReturn(1);

        Map<String, Object> response = bookingService.confirmBookingChanges("b-1", "user-1", request);

        assertEquals("b-1", response.get("bookingId"));
        assertEquals(1, response.get("deletedDocumentCount"));
        verify(documentRetentionCleanupService).cleanupDocumentsForRemovedGuests(
                "b-1",
                List.of("GUEST_3"),
                "BOOKING_EDIT_CONFIRM");
    }

    private static Tour baseTour(String id) {
        Tour tour = new Tour();
        tour.setId(id);
        tour.setHotelName("Le Marais");
        tour.setDestination("Paris");
        tour.setPricePerDuration(Map.of("7 days", "$1000"));
        tour.setMealSupplementsPerDay(Map.of("BB", "$0"));
        tour.setFreeCancellationDaysBefore(7);
        return tour;
    }

    private static CreateBookingRequestDTO request(String userId, String tourId, int adults, String mealPlan) {
        CreateBookingRequestDTO req = new CreateBookingRequestDTO();
        req.setUserId(userId);
        req.setTourId(tourId);
        req.setDate(LocalDate.of(2026, 6, 10));
        req.setDuration("7 days");
        req.setMealPlan(mealPlan);

        BookingGuestsDTO guests = new BookingGuestsDTO();
        guests.setAdult(adults);
        guests.setChildren(1);
        req.setGuests(guests);

        int totalGuests = adults + 1; // adults + 1 child
        List<PersonalDetailDTO> details = new java.util.ArrayList<>();
        for (int i = 0; i < totalGuests; i++) {
            PersonalDetailDTO p = new PersonalDetailDTO();
            p.setFirstName("Person" + (i + 1));
            p.setLastName("Doe");
            details.add(p);
        }
        req.setPersonalDetails(details);

        return req;
    }

    private static Booking booked(String id, String userId, String tourId, String duration, String mealPlan, int adults, int children) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUserId(userId);
        booking.setTourId(tourId);
        booking.setDate(LocalDate.of(2026, 6, 10));
        booking.setDuration(duration);
        booking.setMealPlan(mealPlan);
        booking.setAdults(adults);
        booking.setChildren(children);
        booking.setState(BookingState.BOOKED);
        booking.setTotalPrice("$1000");
        booking.setDocumentCount(0);

        Booking.PersonalDetail lead = new Booking.PersonalDetail();
        lead.setFirstName("Lead");
        lead.setLastName("Traveler");
        booking.setPersonalDetails(List.of(lead));
        return booking;
    }
}
