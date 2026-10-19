package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.DestinationListResponseDTO;
import com.epam.edp.demo.dto.ReviewListResponseDTO;
import com.epam.edp.demo.dto.TourDetailResponseDTO;
import com.epam.edp.demo.dto.TourListResponseDTO;
import com.epam.edp.demo.model.Review;
import com.epam.edp.demo.model.Tour;
import com.epam.edp.demo.repository.ReviewRepository;
import com.epam.edp.demo.repository.TourRepository;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourServiceTest {

    @Mock
    private TourRepository tourRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    private TourService tourService;

    @BeforeEach
    void setUp() {
        tourService = new TourService(tourRepository, reviewRepository, mongoTemplate);
    }

    @Test
    void searchDestinations_throwsWhenQueryTooShort() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tourService.searchDestinations("ab"));

        assertEquals("Query must be at least 3 characters", ex.getMessage());
    }

    @Test
    void searchDestinations_throwsWhenQueryNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tourService.searchDestinations(null));

        assertEquals("Query must be at least 3 characters", ex.getMessage());
    }

    @Test
    void searchDestinations_returnsDistinctDestinations() {
        when(mongoTemplate.findDistinct(any(Query.class), eq("destination"), eq(Tour.class), eq(String.class)))
                .thenReturn(List.of("Paris", "Prague"));

        DestinationListResponseDTO response = tourService.searchDestinations("par");

        assertEquals(2, response.getDestinations().size());
        assertEquals("Paris", response.getDestinations().get(0));
    }

    @Test
    void searchDestinations_acceptsExactlyThreeCharacters() {
        when(mongoTemplate.findDistinct(any(Query.class), eq("destination"), eq(Tour.class), eq(String.class)))
                .thenReturn(List.of("Bali"));

        DestinationListResponseDTO response = tourService.searchDestinations("bal");

        assertEquals(1, response.getDestinations().size());
        assertEquals("Bali", response.getDestinations().get(0));
    }

    @Test
    void getAvailableTours_returnsPagedResult() {
        Tour t1 = tour("t-1", "Paris", LocalDate.of(2026, 7, 1));
        Tour t2 = tour("t-2", "Rome", LocalDate.of(2026, 8, 1));

        when(mongoTemplate.count(any(Query.class), eq(Tour.class))).thenReturn(8L);
        when(mongoTemplate.find(any(Query.class), eq(Tour.class))).thenReturn(List.of(t1, t2));

        TourListResponseDTO response = tourService.getAvailableTours(
            null, null, null, null, null, null, null, null, "RATING_DESC", 2, 2);

        assertEquals(2, response.getPage());
        assertEquals(2, response.getPageSize());
        assertEquals(4, response.getTotalPages());
        assertEquals(8, response.getTotalItems());
        assertEquals(2, response.getTours().size());
    }

    @Test
    void getAvailableTours_addsCapacityExprFilterToQuery() {
        when(mongoTemplate.count(any(Query.class), eq(Tour.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Tour.class))).thenReturn(List.of());

        tourService.getAvailableTours(null, null, null, null, null, null, null, null, "RATING_DESC", 1, 6);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).count(captor.capture(), eq(Tour.class));
        Document q = captor.getValue().getQueryObject();
        assertTrue(q.toJson().contains("$expr"));
        assertTrue(q.toJson().contains("$lt"));
    }

    @Test
    void getAvailableTours_appliesDestinationFilterExceptAnyDestination() {
        when(mongoTemplate.count(any(Query.class), eq(Tour.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Tour.class))).thenReturn(List.of());

        tourService.getAvailableTours("Paris", null, null, null, null, null, null, null, "RATING_DESC", 1, 6);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).count(captor.capture(), eq(Tour.class));
        assertTrue(captor.getValue().getQueryObject().toJson().contains("destination"));
    }

    @Test
    void getAvailableTours_doesNotApplyDestinationFilterForAnyDestinationKeyword() {
        when(mongoTemplate.count(any(Query.class), eq(Tour.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Tour.class))).thenReturn(List.of());

        tourService.getAvailableTours("Any destination", null, null, null, null, null, null, null, "RATING_DESC", 1, 6);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).count(captor.capture(), eq(Tour.class));
        String queryJson = captor.getValue().getQueryObject().toJson();
        assertFalse(queryJson.contains("Any destination"));
    }

    @Test
    void getAvailableTours_appliesGuestCapacityFilter() {
        when(mongoTemplate.count(any(Query.class), eq(Tour.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Tour.class))).thenReturn(List.of());

        tourService.getAvailableTours(null, null, null, null, 2, 1, null, null, "RATING_DESC", 1, 6);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).count(captor.capture(), eq(Tour.class));
        assertTrue(captor.getValue().getQueryObject().toJson().contains("guestQuantity.totalMaxValue"));
    }

    @Test
    void getAvailableTours_defaultsAdultsToOneWhenOnlyChildrenProvided() {
        when(mongoTemplate.count(any(Query.class), eq(Tour.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Tour.class))).thenReturn(List.of());

        tourService.getAvailableTours(null, null, null, null, null, 2, null, null, "RATING_DESC", 1, 6);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).count(captor.capture(), eq(Tour.class));
        assertTrue(captor.getValue().getQueryObject().toJson().contains("$gte"));
        assertTrue(captor.getValue().getQueryObject().toJson().contains("3"));
    }

    @Test
    void getAvailableTours_formatsMealPlansInResponse() {
        Tour t = tour("t-1", "Paris", LocalDate.of(2026, 7, 1));
        t.setMealPlans(List.of("BB", "HB", "RAW"));
        when(mongoTemplate.count(any(Query.class), eq(Tour.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Tour.class))).thenReturn(List.of(t));

        TourListResponseDTO response = tourService.getAvailableTours(
            null, null, null, null, null, null, null, null, "RATING_DESC", 1, 6);

        assertEquals("Breakfast (BB)", response.getTours().get(0).getMealPlans().get(0));
        assertEquals("Half-board (HB)", response.getTours().get(0).getMealPlans().get(1));
        assertEquals("RAW", response.getTours().get(0).getMealPlans().get(2));
    }

    @Test
    void getAvailableTours_computesFreeCancellationFromEarliestStartDate() {
        Tour t = tour("t-1", "Paris", LocalDate.of(2026, 7, 10));
        t.setStartDates(List.of(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 1)));
        t.setFreeCancellationDaysBefore(5);
        when(mongoTemplate.count(any(Query.class), eq(Tour.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Tour.class))).thenReturn(List.of(t));

        TourListResponseDTO response = tourService.getAvailableTours(
            null, null, null, null, null, null, null, null, "RATING_DESC", 1, 6);

        assertEquals(LocalDate.of(2026, 6, 26), response.getTours().get(0).getFreeCancellation());
    }

    @Test
    void getAvailableTours_usesPriceOnRequestWhenNoDurationPriceAvailable() {
        Tour t = tour("t-1", "Paris", LocalDate.of(2026, 7, 1));
        t.setPricePerDuration(Map.of());
        when(mongoTemplate.count(any(Query.class), eq(Tour.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Tour.class))).thenReturn(List.of(t));

        TourListResponseDTO response = tourService.getAvailableTours(
                null, null, null, null, null, null, null, "RATING_DESC", 1, 6);

        assertEquals("Price on request", response.getTours().get(0).getPrice());
    }

    @Test
    void getTourById_returnsNotFoundWhenMissing() {
        when(tourRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tourService.getTourById("missing"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getTourById_returnsMappedDetails() {
        Tour t = tour("t-1", "Paris", LocalDate.of(2026, 7, 1));
        t.setSummary("Great trip");
        t.setImageUrls(List.of("img-1"));
        t.setReviewCount(42);
        t.setDurations(List.of("5 days", "7 days"));
        t.setMealPlans(List.of("BB", "AI"));
        Tour.GuestQuantity guestQuantity = new Tour.GuestQuantity();
        guestQuantity.setAdultsMaxValue(2);
        guestQuantity.setChildrenMaxValue(1);
        guestQuantity.setTotalMaxValue(3);
        t.setGuestQuantity(guestQuantity);
        t.setFreeCancellationDaysBefore(7);
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(t));

        TourDetailResponseDTO response = tourService.getTourById("t-1");

        assertEquals("t-1", response.getId());
        assertEquals("Paris", response.getDestination());
        assertEquals("Breakfast (BB)", response.getMealPlans().get(0));
        assertEquals("All inclusive (AI)", response.getMealPlans().get(1));
        assertEquals(LocalDate.of(2026, 6, 24), response.getFreeCancellationDeadline());
        assertNotNull(response.getGuestQuantity());
        assertEquals(3, response.getGuestQuantity().getTotalMaxValue());
    }

    @Test
    void getTourById_handlesNullMealPlansAndStartDates() {
        Tour t = new Tour();
        t.setId("t-1");
        t.setName("No data");
        t.setDestination("NA");
        t.setMealPlans(null);
        t.setStartDates(null);
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(t));

        TourDetailResponseDTO response = tourService.getTourById("t-1");

        assertTrue(response.getMealPlans().isEmpty());
        assertEquals(null, response.getFreeCancellationDeadline());
    }

    @Test
    void getReviews_throwsNotFoundWhenTourMissing() {
        when(tourRepository.existsById("tour-404")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tourService.getReviews("tour-404", "TOP_RATED_FIRST", 1, 4));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getReviews_returnsPagedAndAverageRating() {
        when(tourRepository.existsById("tour-1")).thenReturn(true);
        when(mongoTemplate.count(any(Query.class), eq(Review.class))).thenReturn(5L);
        Review r1 = review("r1", 5.0, LocalDate.of(2026, 1, 1));
        Review r2 = review("r2", 4.0, LocalDate.of(2026, 1, 2));
        when(mongoTemplate.find(any(Query.class), eq(Review.class))).thenReturn(List.of(r1, r2));
        when(reviewRepository.findByTourIdAndHiddenFalse("tour-1")).thenReturn(List.of(r1, r2, review("r3", 3.0, LocalDate.of(2026, 1, 3))));

        ReviewListResponseDTO response = tourService.getReviews("tour-1", "TOP_RATED_FIRST", 1, 2);

        assertEquals(2, response.getReviews().size());
        assertEquals(1, response.getPage());
        assertEquals(2, response.getPageSize());
        assertEquals(3, response.getTotalPages());
        assertEquals(5, response.getTotalItems());
        assertEquals(4.0, response.getAverageRating());
    }

    @Test
    void getReviews_defaultsSortWhenSortByNull() {
        when(tourRepository.existsById("tour-1")).thenReturn(true);
        when(mongoTemplate.count(any(Query.class), eq(Review.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Review.class))).thenReturn(List.of());
        when(reviewRepository.findByTourIdAndHiddenFalse("tour-1")).thenReturn(List.of());

        tourService.getReviews("tour-1", null, 1, 4);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(Review.class));
        assertEquals(-1, captor.getValue().getSortObject().get("rate"));
    }

    @Test
    void getReviews_supportsOldestFirstSort() {
        when(tourRepository.existsById("tour-1")).thenReturn(true);
        when(mongoTemplate.count(any(Query.class), eq(Review.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Review.class))).thenReturn(List.of());
        when(reviewRepository.findByTourIdAndHiddenFalse("tour-1")).thenReturn(List.of());

        tourService.getReviews("tour-1", "OLDEST_FIRST", 1, 4);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(Review.class));
        assertEquals(1, captor.getValue().getSortObject().get("reviewDate"));
    }

    @Test
    void getReviews_supportsNewestFirstSort() {
        when(tourRepository.existsById("tour-1")).thenReturn(true);
        when(mongoTemplate.count(any(Query.class), eq(Review.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Review.class))).thenReturn(List.of());
        when(reviewRepository.findByTourIdAndHiddenFalse("tour-1")).thenReturn(List.of());

        tourService.getReviews("tour-1", "NEWEST_FIRST", 1, 4);

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(Review.class));
        assertEquals(-1, captor.getValue().getSortObject().get("reviewDate"));
    }

    @Test
    void getReviews_returnsNullAverageWhenNoRatedReviews() {
        when(tourRepository.existsById("tour-1")).thenReturn(true);
        when(mongoTemplate.count(any(Query.class), eq(Review.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Review.class))).thenReturn(List.of());
        Review unrated = review("r1", null, LocalDate.of(2026, 1, 1));
        when(reviewRepository.findByTourIdAndHiddenFalse("tour-1")).thenReturn(List.of(unrated));

        ReviewListResponseDTO response = tourService.getReviews("tour-1", "TOP_RATED_FIRST", 1, 4);

        assertEquals(null, response.getAverageRating());
    }

    @Test
    void getReviews_roundsAverageToTwoDecimals() {
        when(tourRepository.existsById("tour-1")).thenReturn(true);
        when(mongoTemplate.count(any(Query.class), eq(Review.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Review.class))).thenReturn(List.of());
        when(reviewRepository.findByTourIdAndHiddenFalse("tour-1")).thenReturn(List.of(
                review("r1", 4.3333, LocalDate.of(2026, 1, 1)),
                review("r2", 4.3333, LocalDate.of(2026, 1, 2))
        ));

        ReviewListResponseDTO response = tourService.getReviews("tour-1", "TOP_RATED_FIRST", 1, 4);

        assertEquals(4.33, response.getAverageRating());
    }

    @Test
    void getAvailableTours_supportsPriceAscSortWithoutError() {
        when(mongoTemplate.find(any(Query.class), eq(Tour.class))).thenReturn(List.of());

        TourListResponseDTO response = tourService.getAvailableTours(
            null, null, null, null, null, null, null, null, "PRICE_ASC", 1, 6);

        assertEquals(0, response.getTotalItems());
    }

    @Test
    void getAvailableTours_supportsPriceDescSortWithoutError() {
        when(mongoTemplate.find(any(Query.class), eq(Tour.class))).thenReturn(List.of());

        TourListResponseDTO response = tourService.getAvailableTours(
            null, null, null, null, null, null, null, null, "PRICE_DESC", 1, 6);

        assertEquals(0, response.getTotalItems());
    }

    private static Tour tour(String id, String destination, LocalDate start) {
        Tour tour = new Tour();
        tour.setId(id);
        tour.setName("Trip " + id);
        tour.setDestination(destination);
        tour.setStartDates(List.of(start));
        tour.setDurations(List.of("7 days"));
        tour.setMealPlans(List.of("BB"));
        tour.setPricePerDuration(Map.of("7 days", "$200"));
        tour.setMealSupplementsPerDay(Map.of("BB", "$0"));
        tour.setRating(4.7);
        tour.setReviewCount(10);
        tour.setFreeCancellationDaysBefore(3);
        tour.setTotalCapacity(20);
        tour.setBookedCount(10);
        return tour;
    }

    private static Review review(String id, Double rate, LocalDate date) {
        Review r = new Review();
        r.setId(id);
        r.setTourId("tour-1");
        r.setUserName("User " + id);
        r.setUserAvatarUrl("avatar-" + id);
        r.setRate(rate);
        r.setComment("comment " + id);
        r.setReviewDate(date);
        r.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return r;
    }
}
