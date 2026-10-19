package com.epam.edp.demo.service;
import com.epam.edp.demo.util.MealPlanFormatter;
import com.epam.edp.demo.dto.DestinationListResponseDTO;
import com.epam.edp.demo.dto.ReviewListResponseDTO;
import com.epam.edp.demo.dto.TourDetailResponseDTO;
import com.epam.edp.demo.dto.TourListResponseDTO;
import com.epam.edp.demo.model.Review;
import com.epam.edp.demo.model.Tour;
import com.epam.edp.demo.repository.ReviewRepository;
import com.epam.edp.demo.repository.TourRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.bson.Document;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TourService {

    private static final int    MIN_QUERY_LENGTH  = 3;
    private static final double ROUND_FACTOR      = 100.0;

    private static final String FIELD_DESTINATION = "destination";
    private static final String FIELD_START_DATES = "startDates";
    private static final String FIELD_RATING      = "rating";
    private static final String FIELD_BASE_PRICE  = "basePrice";
    private static final String FIELD_CREATED_AT  = "createdAt";
    private static final String FIELD_TOUR_ID     = "tourId";
    private static final String SORT_RATING_ASC   = "RATING_ASC";
    private static final String ANY_DESTINATION   = "Any destination";

    private static final Set<String> VALID_SORT_VALUES = Set.of(
            "RATING_ASC", "RATING_DESC", "PRICE_ASC", "PRICE_DESC", "DATE_ASC");
    private static final Set<String> VALID_MEAL_PLANS = Set.of(
            "BB", "HB", "FB", "AI", "RO");
    private static final Set<String> VALID_TOUR_TYPES = Set.of(
            "Cruises", "Hikes", "Resorts");
    private static final Set<String> VALID_REVIEW_SORT_VALUES = Set.of(
            "TOP_RATED_FIRST", "LOW_RATED_FIRST", "NEWEST_FIRST", "OLDEST_FIRST");

    private final TourRepository tourRepository;
    private final ReviewRepository reviewRepository;
    private final MongoTemplate mongoTemplate;

    public TourService(TourRepository tourRepository,
                       ReviewRepository reviewRepository,
                       MongoTemplate mongoTemplate) {
        this.tourRepository = tourRepository;
        this.reviewRepository = reviewRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public DestinationListResponseDTO searchDestinations(String query) {
        if (query == null || query.isBlank() || query.trim().length() < MIN_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "Query must be at least 3 characters"
            );
        }

        List<String> destinations = mongoTemplate.findDistinct(
                new Query(Criteria.where(FIELD_DESTINATION)
                        .regex(Pattern.quote(query), "i")),
                FIELD_DESTINATION,
                Tour.class,
                String.class
        );

        return DestinationListResponseDTO.builder()
                .destinations(destinations)
                .build();
    }

    public TourListResponseDTO getAvailableTours(
            String destination,
            LocalDate startDate,
            LocalDate endDate,
            String duration,
            Integer adults,
            Integer children,
            String mealPlan,
            String tourType,
            String sortBy,
            int page,
            int pageSize
    ) {
        if (mealPlan != null && !VALID_MEAL_PLANS.contains(mealPlan)) {
            throw new IllegalArgumentException("Invalid mealPlan: " + mealPlan
                    + ". Allowed values: " + VALID_MEAL_PLANS);
        }
        if (tourType != null && !VALID_TOUR_TYPES.contains(tourType)) {
            throw new IllegalArgumentException("Invalid tourType: " + tourType
                    + ". Allowed values: " + VALID_TOUR_TYPES);
        }
        if (sortBy != null && !VALID_SORT_VALUES.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sortBy: " + sortBy
                    + ". Allowed values: " + VALID_SORT_VALUES);
        }

        Query query = buildTourQuery(
                destination, startDate, endDate, duration,
                adults, children, mealPlan, tourType
        );

        if (page < 1) {
            throw new IllegalArgumentException("Page must be >= 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be >= 1");
        }

        boolean inMemorySort = "PRICE_ASC".equals(sortBy)
                || "PRICE_DESC".equals(sortBy)
                || "DATE_ASC".equals(sortBy);

        long totalItems;
        List<Tour> tours;

        if (inMemorySort) {
            // Price and date sorts need in-memory handling because
            // prices are stored as strings in pricePerDuration map
            // and there is no basePrice field in the database.
            List<Tour> allTours = new java.util.ArrayList<>(mongoTemplate.find(query, Tour.class));
            totalItems = allTours.size();

            Comparator<Tour> comparator = switch (sortBy) {
                case "PRICE_ASC"  -> Comparator.comparingDouble(this::getLowestPrice);
                case "PRICE_DESC" -> Comparator.comparingDouble(this::getLowestPrice).reversed();
                case "DATE_ASC"   -> Comparator.comparing(this::getEarliestStartDate,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                default -> Comparator.comparingDouble(this::getLowestPrice);
            };

            allTours.sort(comparator);

            int fromIndex = (page - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, allTours.size());
            tours = fromIndex < allTours.size() ? allTours.subList(fromIndex, toIndex) : List.of();
        } else {
            Sort sort = applySortOrder(sortBy);
            totalItems = mongoTemplate.count(query, Tour.class);
            query.with(PageRequest.of(page - 1, pageSize, sort));
            tours = mongoTemplate.find(query, Tour.class);
        }

        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        List<TourListResponseDTO.TourItem> tourItems = tours.stream()
                .map(this::mapToTourItem)
                .collect(Collectors.toList());

        return TourListResponseDTO.builder()
                .tours(tourItems)
                .page(page)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .totalItems((int) totalItems)
                .build();
    }

    public TourListResponseDTO getAvailableTours(
            String destination,
            LocalDate startDate,
            String duration,
            Integer adults,
            Integer children,
            String mealPlan,
            String tourType,
            String sortBy,
            int page,
            int pageSize
    ) {
        return getAvailableTours(
                destination,
                startDate,
                null,
                duration,
                adults,
                children,
                mealPlan,
                tourType,
                sortBy,
                page,
                pageSize
        );
    }

    private Query buildTourQuery(
            String destination,
            LocalDate startDate,
            LocalDate endDate,
            String duration,
            Integer adults,
            Integer children,
            String mealPlan,
            String tourType
    ) {
        // Use BasicQuery so $expr is sent as raw BSON —
        // Criteria.where("$expr") escapes $ in Spring Data MongoDB 4.x
        Document capacityFilter = new Document("$expr",
                new Document("$lt", Arrays.asList("$bookedCount", "$totalCapacity")));
        Query query = new BasicQuery(capacityFilter);

        if (destination != null
                && !destination.isBlank()
                && !ANY_DESTINATION.equalsIgnoreCase(destination)) {
            query.addCriteria(
                    Criteria.where(FIELD_DESTINATION).regex(Pattern.quote(destination), "i")
            );
        }

        if (startDate != null) {
            if (endDate != null) {
                // Filter tours with start dates within the range
                Criteria dateCriteria = new Criteria().gte(startDate).lte(endDate);
                query.addCriteria(
                        Criteria.where(FIELD_START_DATES).elemMatch(dateCriteria)
                );
            } else {
                // Single date - exact match
                query.addCriteria(
                        Criteria.where(FIELD_START_DATES).in(startDate)
                );
            }
        }

        if (duration != null) {
            query.addCriteria(
                    Criteria.where("durations").in(duration)
            );
        }

        if (mealPlan != null) {
            query.addCriteria(
                    Criteria.where("mealPlans").in(mealPlan)
            );
        }

        if (tourType != null) {
            query.addCriteria(
                    Criteria.where("tourType").is(tourType)
            );
        }

        if (adults != null || children != null) {
            int totalGuests = (adults  != null ? adults  : 1)
                    + (children != null ? children : 0);
            query.addCriteria(
                    Criteria.where("guestQuantity.totalMaxValue").gte(totalGuests)
            );
        }

        return query;
    }

    private Sort applySortOrder(String sortBy) {
        if (sortBy == null) {
            return Sort.by(Sort.Direction.DESC, FIELD_RATING);
        }
        return switch (sortBy) {
            case "RATING_ASC" -> Sort.by(Sort.Direction.ASC,  "rating");
            default           -> Sort.by(Sort.Direction.DESC, "rating");
        };
    }

    private double getLowestPrice(Tour tour) {
        if (tour.getPricePerDuration() == null || tour.getPricePerDuration().isEmpty()) {
            return Double.MAX_VALUE;
        }
        return tour.getPricePerDuration().values().stream()
                .map(p -> p.replaceAll("[^0-9.]", ""))
                .filter(s -> !s.isEmpty())
                .mapToDouble(Double::parseDouble)
                .min()
                .orElse(Double.MAX_VALUE);
    }

    private LocalDate getEarliestStartDate(Tour tour) {
        if (tour.getStartDates() == null || tour.getStartDates().isEmpty()) {
            return null;
        }
        return tour.getStartDates().stream()
                .filter(d -> d != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private Sort applyReviewSortOrder(String sortBy) {
        if (sortBy == null) {
            return Sort.by(Sort.Direction.DESC, "rate");
        }
        return switch (sortBy) {
            case "RATING_ASC" -> Sort.by(Sort.Direction.ASC,  "rate");
            case "NEWEST"     -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "OLDEST"     -> Sort.by(Sort.Direction.ASC,  "createdAt");
            default           -> Sort.by(Sort.Direction.DESC, "rate");
        };
    }

    // ─────────────────────────────────────────────
    // Private — mappers
    // ─────────────────────────────────────────────
    private TourListResponseDTO.TourItem mapToTourItem(Tour tour) {
        List<LocalDate> dates = tour.getStartDates();
        LocalDate earliestDate = (dates == null || dates.isEmpty()) ? null :
                dates.stream().filter(d -> d != null).min(Comparator.naturalOrder()).orElse(null);

        String lowestPrice = (tour.getPricePerDuration() == null || tour.getPricePerDuration().isEmpty())
                ? "Price on request"
                : tour.getPricePerDuration().values().stream()
                        .min(Comparator.naturalOrder())
                        .map(p -> "from " + p + " for 1 person")
                        .orElse("Price on request");

        LocalDate freeCancellation = (earliestDate != null && tour.getFreeCancellationDaysBefore() != null)
                ? earliestDate.minusDays(tour.getFreeCancellationDaysBefore())
                : null;

        List<String> formattedMealPlans = (tour.getMealPlans() == null) ? List.of() :
                tour.getMealPlans().stream()
                        .map(MealPlanFormatter::format)
                        .collect(Collectors.toList());

        return TourListResponseDTO.TourItem.builder()
                .id(tour.getId())
                .name(tour.getName())
                .destination(tour.getDestination())
                .startDate(earliestDate)
                .durations(tour.getDurations())
                .mealPlans(formattedMealPlans)
                .price(lowestPrice)
                .rating(tour.getRating())
                .reviews(tour.getReviewCount())
                .freeCancellation(freeCancellation)
                .build();
    }





    public TourDetailResponseDTO getTourById(String id) {
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tour not found: " + id));

        LocalDate earliest = tour.getStartDates() == null ? null :
                tour.getStartDates().stream()
                        .min(Comparator.naturalOrder())
                        .orElse(null);

        LocalDate freeCancellationDeadline = (earliest != null
                && tour.getFreeCancellationDaysBefore() != null)
                ? earliest.minusDays(tour.getFreeCancellationDaysBefore())
                : null;

        List<String> formattedMealPlans = tour.getMealPlans() == null ? List.of() :
                tour.getMealPlans().stream()
                        .map(MealPlanFormatter::format)
                        .collect(Collectors.toList());

        TourDetailResponseDTO.GuestQuantityDTO guestQuantityDTO = null;
        if (tour.getGuestQuantity() != null) {
            guestQuantityDTO = new TourDetailResponseDTO.GuestQuantityDTO(
                    tour.getGuestQuantity().getAdultsMaxValue(),
                    tour.getGuestQuantity().getChildrenMaxValue(),
                    tour.getGuestQuantity().getTotalMaxValue()
            );
        }

        return TourDetailResponseDTO.builder()
                .id(tour.getId())
                .name(tour.getName())
                .destination(tour.getDestination())
                .summary(tour.getSummary())
                .imageUrls(tour.getImageUrls())
                .rating(tour.getRating())
                .reviewCount(tour.getReviewCount())
                .startDates(tour.getStartDates())
                .durations(tour.getDurations())
                .pricePerDuration(tour.getPricePerDuration())
                .mealPlans(formattedMealPlans)
                .mealSupplementsPerDay(tour.getMealSupplementsPerDay())
                .hotelName(tour.getHotelName())
                .hotelDescription(tour.getHotelDescription())
                .accommodation(tour.getAccommodation())
                .tourType(tour.getTourType())
                .customDetails(tour.getCustomDetails())
                .guestQuantity(guestQuantityDTO)
                .freeCancellationDaysBefore(tour.getFreeCancellationDaysBefore())
                .freeCancellationDeadline(freeCancellationDeadline)
                .build();
    }

    public ReviewListResponseDTO getReviews(String tourId, String sortBy, int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be >= 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be >= 1");
        }

        if (sortBy != null && !VALID_REVIEW_SORT_VALUES.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sortBy: " + sortBy
                    + ". Allowed values: " + VALID_REVIEW_SORT_VALUES);
        }

        if (!tourRepository.existsById(tourId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found: " + tourId);
        }

        Sort sort = switch (sortBy == null ? "TOP_RATED_FIRST" : sortBy) {
            case "LOW_RATED_FIRST" -> Sort.by(Sort.Direction.ASC,  "rate");
            case "NEWEST_FIRST"   -> Sort.by(Sort.Direction.DESC, "reviewDate");
            case "OLDEST_FIRST"   -> Sort.by(Sort.Direction.ASC,  "reviewDate");
            default               -> Sort.by(Sort.Direction.DESC, "rate"); // TOP_RATED_FIRST
        };

        Query countQuery = new Query(Criteria.where(FIELD_TOUR_ID).is(tourId)
                .and("hidden").ne(true));
        long totalItems = mongoTemplate.count(countQuery, Review.class);

        Query pagedQuery = new Query(Criteria.where(FIELD_TOUR_ID).is(tourId)
                .and("hidden").ne(true))
                .with(PageRequest.of(page - 1, pageSize, sort));
        List<Review> reviews = mongoTemplate.find(pagedQuery, Review.class);

        List<Review> allReviews = reviewRepository.findByTourIdAndHiddenFalse(tourId);
        OptionalDouble avg = allReviews.stream()
                .filter(r -> r.getRate() != null)
                .mapToDouble(Review::getRate)
                .average();
        Double averageRating = avg.isPresent()
                ? (Math.round(avg.getAsDouble() * ROUND_FACTOR) / ROUND_FACTOR)
                : null;

        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        List<ReviewListResponseDTO.ReviewItem> items = reviews.stream()
                .map(r -> ReviewListResponseDTO.ReviewItem.builder()
                        .id(r.getId())
                        .userName(r.getUserName())
                        .userAvatarUrl(r.getUserAvatarUrl())
                        .rate(r.getRate())
                        .comment(r.getComment())
                        .reviewDate(r.getReviewDate())
                        .build())
                .collect(Collectors.toList());

        return ReviewListResponseDTO.builder()
                .reviews(items)
                .page(page)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .totalItems((int) totalItems)
                .averageRating(averageRating)
                .build();
    }
}