package com.epam.edp.demo.repository;

import com.epam.edp.demo.enums.FeedbackStatus;
import com.epam.edp.demo.model.Feedback;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends MongoRepository<Feedback, String> {

    Optional<Feedback> findByBookingId(String bookingId);

    List<Feedback> findByTourId(String tourId);

    List<Feedback> findByTourIdAndStatus(String tourId, FeedbackStatus status);

    boolean existsByBookingId(String bookingId);

    Optional<Feedback> findByBookingIdAndCustomerId(String bookingId, String customerId);
}
