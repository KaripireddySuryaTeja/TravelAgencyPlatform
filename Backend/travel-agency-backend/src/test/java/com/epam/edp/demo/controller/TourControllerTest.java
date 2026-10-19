package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.DestinationListResponseDTO;
import com.epam.edp.demo.dto.ReviewListResponseDTO;
import com.epam.edp.demo.dto.TourDetailResponseDTO;
import com.epam.edp.demo.dto.TourListResponseDTO;
import com.epam.edp.demo.service.TourService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourControllerTest {

    @Mock
    private TourService tourService;

    private TourController tourController;

    @BeforeEach
    void setUp() {
        tourController = new TourController(tourService);
    }

    @Test
    void getDestinations_returnsOkAndBody() {
        DestinationListResponseDTO dto = new DestinationListResponseDTO(List.of("Paris", "Rome"));
        when(tourService.searchDestinations("par")).thenReturn(dto);

        ResponseEntity<DestinationListResponseDTO> response = tourController.getDestinations("par");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void getDestinations_delegatesInputToService() {
        when(tourService.searchDestinations("tok")).thenReturn(new DestinationListResponseDTO(List.of("Tokyo")));

        tourController.getDestinations("tok");

        verify(tourService).searchDestinations("tok");
    }

    @Test
    void getDestinations_propagatesServiceException() {
        when(tourService.searchDestinations("ab")).thenThrow(new IllegalArgumentException("too short"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tourController.getDestinations("ab"));

        assertEquals("too short", ex.getMessage());
    }

    @Test
    void getAvailableTours_returnsOkAndBody() {
        TourListResponseDTO dto = TourListResponseDTO.builder().tours(List.of()).build();
        when(tourService.getAvailableTours("Paris", LocalDate.of(2026, 6, 1), null, "7 days", 2, 1, "BB", "City", "RATING_DESC", 1, 6))
                .thenReturn(dto);

        ResponseEntity<TourListResponseDTO> response = tourController.getAvailableTours(
            "Paris", LocalDate.of(2026, 6, 1), null, "7 days", 2, 1, "BB", "City", "RATING_DESC", 1, 6);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void getAvailableTours_delegatesAllFiltersToService() {
        when(tourService.getAvailableTours("Rome", LocalDate.of(2026, 7, 1), null, "5 days", 1, 0, "HB", "Heritage", "PRICE_ASC", 2, 10))
                .thenReturn(TourListResponseDTO.builder().tours(List.of()).build());

        tourController.getAvailableTours("Rome", LocalDate.of(2026, 7, 1), null, "5 days", 1, 0, "HB", "Heritage", "PRICE_ASC", 2, 10);

        verify(tourService).getAvailableTours("Rome", LocalDate.of(2026, 7, 1), null, "5 days", 1, 0, "HB", "Heritage", "PRICE_ASC", 2, 10);
    }

    @Test
    void getAvailableTours_acceptsNullOptionalFilters() {
        when(tourService.getAvailableTours(null, null, null, null, null, null, null, null, "RATING_DESC", 1, 6))
                .thenReturn(TourListResponseDTO.builder().tours(List.of()).build());

        ResponseEntity<TourListResponseDTO> response = tourController.getAvailableTours(
            null, null, null, null, null, null, null, null, "RATING_DESC", 1, 6);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tourService).getAvailableTours(null, null, null, null, null, null, null, null, "RATING_DESC", 1, 6);
    }

    @Test
    void getAvailableTours_propagatesServiceExceptions() {
        when(tourService.getAvailableTours(null, null, null, null, null, null, null, null, "RATING_DESC", 1, 6))
                .thenThrow(new RuntimeException("search failure"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> tourController.getAvailableTours(null, null, null, null, null, null, null, null, "RATING_DESC", 1, 6));

        assertEquals("search failure", ex.getMessage());
    }

    @Test
    void getTourById_returnsOkAndBody() {
        TourDetailResponseDTO dto = TourDetailResponseDTO.builder().id("aaaaaaaaaaaaaaaaaaaaaaaa").name("Paris").build();
        when(tourService.getTourById("aaaaaaaaaaaaaaaaaaaaaaaa")).thenReturn(dto);

        ResponseEntity<TourDetailResponseDTO> response = tourController.getTourById("aaaaaaaaaaaaaaaaaaaaaaaa");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void getTourById_delegatesIdToService() {
        when(tourService.getTourById("bbbbbbbbbbbbbbbbbbbbbbbb")).thenReturn(TourDetailResponseDTO.builder().id("bbbbbbbbbbbbbbbbbbbbbbbb").build());

        tourController.getTourById("bbbbbbbbbbbbbbbbbbbbbbbb");

        verify(tourService).getTourById("bbbbbbbbbbbbbbbbbbbbbbbb");
    }

    @Test
    void getTourById_propagatesServiceExceptions() {
        when(tourService.getTourById("cccccccccccccccccccccccc")).thenThrow(new RuntimeException("not found"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> tourController.getTourById("cccccccccccccccccccccccc"));

        assertEquals("not found", ex.getMessage());
    }

    @Test
    void getReviews_returnsOkAndBody() {
        ReviewListResponseDTO dto = ReviewListResponseDTO.builder().reviews(List.of()).build();
        when(tourService.getReviews("aaaaaaaaaaaaaaaaaaaaaaaa", "TOP_RATED_FIRST", 1, 4)).thenReturn(dto);

        ResponseEntity<ReviewListResponseDTO> response = tourController.getReviews("aaaaaaaaaaaaaaaaaaaaaaaa", "TOP_RATED_FIRST", 1, 4);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void getReviews_delegatesInputsToService() {
        when(tourService.getReviews("dddddddddddddddddddddddd", "NEWEST_FIRST", 2, 8)).thenReturn(ReviewListResponseDTO.builder().reviews(List.of()).build());

        tourController.getReviews("dddddddddddddddddddddddd", "NEWEST_FIRST", 2, 8);

        verify(tourService).getReviews("dddddddddddddddddddddddd", "NEWEST_FIRST", 2, 8);
    }

    @Test
    void getReviews_propagatesServiceExceptions() {
        when(tourService.getReviews("aaaaaaaaaaaaaaaaaaaaaaaa", "TOP_RATED_FIRST", 1, 4)).thenThrow(new RuntimeException("reviews error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> tourController.getReviews("aaaaaaaaaaaaaaaaaaaaaaaa", "TOP_RATED_FIRST", 1, 4));

        assertEquals("reviews error", ex.getMessage());
    }

    @Test
    void getAvailableTours_passesSortByPriceDesc() {
        when(tourService.getAvailableTours(null, null, null, null, null, null, null, null, "PRICE_DESC", 1, 6))
                .thenReturn(TourListResponseDTO.builder().tours(List.of()).build());

        tourController.getAvailableTours(null, null, null, null, null, null, null, null, "PRICE_DESC", 1, 6);

        verify(tourService).getAvailableTours(null, null, null, null, null, null, null, null, "PRICE_DESC", 1, 6);
    }

    @Test
    void getAvailableTours_passesSortByPriceAsc() {
        when(tourService.getAvailableTours(null, null, null, null, null, null, null, null, "PRICE_ASC", 1, 6))
                .thenReturn(TourListResponseDTO.builder().tours(List.of()).build());

        tourController.getAvailableTours(null, null, null, null, null, null, null, null, "PRICE_ASC", 1, 6);

        verify(tourService).getAvailableTours(null, null, null, null, null, null, null, null, "PRICE_ASC", 1, 6);
    }

    @Test
    void getAvailableTours_passesCustomPagingValues() {
        when(tourService.getAvailableTours(null, null, null, null, null, null, null, null, "RATING_DESC", 3, 12))
                .thenReturn(TourListResponseDTO.builder().tours(List.of()).build());

        ResponseEntity<TourListResponseDTO> response = tourController.getAvailableTours(
            null, null, null, null, null, null, null, null, "RATING_DESC", 3, 12);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tourService).getAvailableTours(null, null, null, null, null, null, null, null, "RATING_DESC", 3, 12);
    }

    @Test
    void getAvailableTours_forwardsZeroAndNegativePagingValues() {
        when(tourService.getAvailableTours(null, null, null, null, null, null, null, null, "RATING_DESC", 0, -5))
                .thenReturn(TourListResponseDTO.builder().tours(List.of()).build());

        ResponseEntity<TourListResponseDTO> response = tourController.getAvailableTours(
            null, null, null, null, null, null, null, null, "RATING_DESC", 0, -5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tourService).getAvailableTours(null, null, null, null, null, null, null, null, "RATING_DESC", 0, -5);
    }

    @Test
    void getTourById_returnsNonNullBodyForValidServiceResponse() {
        when(tourService.getTourById("aaaaaaaaaaaaaaaaaaaaaaaa")).thenReturn(TourDetailResponseDTO.builder().id("aaaaaaaaaaaaaaaaaaaaaaaa").build());

        ResponseEntity<TourDetailResponseDTO> response = tourController.getTourById("aaaaaaaaaaaaaaaaaaaaaaaa");

        assertNotNull(response.getBody());
    }

    @Test
    void getReviews_returnsNonNullBodyForValidServiceResponse() {
        when(tourService.getReviews("aaaaaaaaaaaaaaaaaaaaaaaa", "TOP_RATED_FIRST", 1, 4))
                .thenReturn(ReviewListResponseDTO.builder().reviews(List.of()).build());

        ResponseEntity<ReviewListResponseDTO> response = tourController.getReviews("aaaaaaaaaaaaaaaaaaaaaaaa", "TOP_RATED_FIRST", 1, 4);

        assertNotNull(response.getBody());
    }

    @Test
    void getDestinations_handlesDifferentQueryInput() {
        when(tourService.searchDestinations("san")).thenReturn(new DestinationListResponseDTO(List.of("Santorini")));

        ResponseEntity<DestinationListResponseDTO> response = tourController.getDestinations("san");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getDestinations().size());
    }

    @Test
    void getReviews_handlesDifferentSortOption() {
        when(tourService.getReviews("eeeeeeeeeeeeeeeeeeeeeeee", "OLDEST_FIRST", 1, 4))
                .thenReturn(ReviewListResponseDTO.builder().reviews(List.of()).build());

        ResponseEntity<ReviewListResponseDTO> response = tourController.getReviews("eeeeeeeeeeeeeeeeeeeeeeee", "OLDEST_FIRST", 1, 4);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tourService).getReviews("eeeeeeeeeeeeeeeeeeeeeeee", "OLDEST_FIRST", 1, 4);
    }

    @Test
    void getAvailableTours_forwardsDateOnlyFilterRequest() {
        LocalDate date = LocalDate.of(2026, 12, 25);
        when(tourService.getAvailableTours(null, date, null, null, null, null, null, null, "RATING_DESC", 1, 6))
                .thenReturn(TourListResponseDTO.builder().tours(List.of()).build());

        ResponseEntity<TourListResponseDTO> response = tourController.getAvailableTours(
            null, date, null, null, null, null, null, null, "RATING_DESC", 1, 6);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tourService).getAvailableTours(null, date, null, null, null, null, null, null, "RATING_DESC", 1, 6);
    }
}
