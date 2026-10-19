package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.SalesReportRowDTO;
import com.epam.edp.demo.dto.StaffPerformanceRowDTO;
import com.epam.edp.demo.enums.BookingState;
import com.epam.edp.demo.enums.Role;
import com.epam.edp.demo.model.Booking;
import com.epam.edp.demo.model.Review;
import com.epam.edp.demo.model.Tour;
import com.epam.edp.demo.model.User;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.repository.ReviewRepository;
import com.epam.edp.demo.repository.TourRepository;
import com.epam.edp.demo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final BookingRepository bookingRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public ReportService(BookingRepository bookingRepository,
                         TourRepository tourRepository,
                         UserRepository userRepository,
                         ReviewRepository reviewRepository) {
        this.bookingRepository = bookingRepository;
        this.tourRepository = tourRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    public List<StaffPerformanceRowDTO> getStaffPerformance(LocalDate from, LocalDate to, String location) {
        List<User> agents = userRepository.findByRole(Role.TRAVEL_AGENT);

        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(periodDays - 1);

        List<StaffPerformanceRowDTO> result = new ArrayList<>();

        for (User agent : agents) {
            List<Tour> agentTours = tourRepository.findByAssignedAgentId(agent.getId());

            if (location != null && !location.isBlank()) {
                agentTours = agentTours.stream()
                        .filter(t -> location.equalsIgnoreCase(t.getDestination()))
                        .collect(Collectors.toList());
            }

            if (agentTours.isEmpty()) continue;

            List<String> tourIds = agentTours.stream().map(Tour::getId).collect(Collectors.toList());

            List<Booking> currentBookings = bookingRepository
                    .findByTourIdInAndDateBetween(tourIds, from, to)
                    .stream()
                    .filter(b -> b.getState() != BookingState.CANCELLED)
                    .collect(Collectors.toList());

            List<Booking> prevBookings = bookingRepository
                    .findByTourIdInAndDateBetween(tourIds, prevFrom, prevTo)
                    .stream()
                    .filter(b -> b.getState() != BookingState.CANCELLED)
                    .collect(Collectors.toList());

            List<Review> reviews = reviewRepository.findByTourIdIn(tourIds);

            int toursSold = currentBookings.size();
            int prevToursSold = prevBookings.size();

            double avgFeedback = reviews.stream()
                    .mapToDouble(Review::getRate)
                    .average()
                    .orElse(0.0);
            double minFeedback = reviews.stream()
                    .mapToDouble(Review::getRate)
                    .min()
                    .orElse(0.0);

            result.add(new StaffPerformanceRowDTO(
                    agent.getId(),
                    (agent.getFirstName() + " " + agent.getLastName()).trim(),
                    agent.getEmail(),
                    from,
                    to,
                    toursSold,
                    computePercentDelta(prevToursSold, toursSold),
                    round1(avgFeedback),
                    round1(minFeedback),
                    computePercentDelta((int) (prevToursSold > 0 ? prevToursSold : 0), toursSold),
                    reviews.size()
            ));
        }

        return result;
    }

    public List<SalesReportRowDTO> getSalesReport(LocalDate from, LocalDate to, String location) {
        List<Tour> tours;
        if (location != null && !location.isBlank()) {
            tours = tourRepository.findByDestination(location);
        } else {
            tours = tourRepository.findAll();
        }

        if (tours.isEmpty()) return new ArrayList<>();

        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(periodDays - 1);

        List<String> tourIds = tours.stream().map(Tour::getId).collect(Collectors.toList());

        Map<String, List<Booking>> currentByTour = bookingRepository
                .findByTourIdInAndDateBetween(tourIds, from, to)
                .stream()
                .filter(b -> b.getState() != BookingState.CANCELLED)
                .collect(Collectors.groupingBy(Booking::getTourId));

        Map<String, List<Booking>> prevByTour = bookingRepository
                .findByTourIdInAndDateBetween(tourIds, prevFrom, prevTo)
                .stream()
                .filter(b -> b.getState() != BookingState.CANCELLED)
                .collect(Collectors.groupingBy(Booking::getTourId));

        List<SalesReportRowDTO> result = new ArrayList<>();
        for (Tour tour : tours) {
            List<Booking> curr = currentByTour.getOrDefault(tour.getId(), List.of());
            List<Booking> prev = prevByTour.getOrDefault(tour.getId(), List.of());

            if (curr.isEmpty() && prev.isEmpty()) continue;

            int bookingsCount = curr.size();
            int prevCount = prev.size();
            int totalGuests = curr.stream().mapToInt(b -> b.getAdults() + b.getChildren()).sum();
            int prevGuests = prev.stream().mapToInt(b -> b.getAdults() + b.getChildren()).sum();

            result.add(new SalesReportRowDTO(
                    tour.getId(),
                    tour.getName(),
                    tour.getDestination(),
                    from,
                    to,
                    bookingsCount,
                    computePercentDelta(prevCount, bookingsCount),
                    totalGuests,
                    computePercentDelta(prevGuests, totalGuests)
            ));
        }

        return result;
    }

    public List<String> getLocations() {
        return tourRepository.findAll().stream()
                .map(Tour::getDestination)
                .filter(d -> d != null && !d.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private String computePercentDelta(int prev, int curr) {
        if (prev == 0 && curr == 0) return "0%";
        if (prev == 0) return "N/A";
        double delta = ((double) (curr - prev) / prev) * 100;
        return (delta >= 0 ? "+" : "") + Math.round(delta) + "%";
    }

    private Double round1(double d) {
        if (d == 0.0) return null;
        return Math.round(d * 10.0) / 10.0;
    }
}
