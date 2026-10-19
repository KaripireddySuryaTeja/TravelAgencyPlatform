package com.epam.edp.demo.repository;

import com.epam.edp.demo.model.Tour;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TourRepository extends MongoRepository<Tour, String> {

    // ─────────────────────────────────────────────
    // US4 — Destination autocomplete
    // ─────────────────────────────────────────────

    @Query(value = "{ 'destination': { $regex: ?0, $options: 'i' } }",
            fields = "{ 'destination': 1 }")
    List<Tour> findByDestinationRegex(String query);

    List<Tour> findByAssignedAgentId(String assignedAgentId);

    List<Tour> findByDestination(String destination);


    // ─────────────────────────────────────────────
    // US4 — Available tours with dynamic filtering
    // Used by TourService.getAvailableTours()
    // The actual dynamic query is built in TourService
    // using MongoTemplate — not here
    // ─────────────────────────────────────────────

    // findById is inherited from MongoRepository


    // ─────────────────────────────────────────────
    // US4 — Check if tour is fully booked
    // Used in TourService.buildTourQuery()
    // bookedCount < totalCapacity handled via MongoTemplate
    // ─────────────────────────────────────────────

    @Query("{ '_id': ?0, $expr: { $lt: ['$bookedCount', '$totalCapacity'] } }")
    Optional<Tour> findAvailableTourById(String tourId);


    // ─────────────────────────────────────────────
    // US6 — Increment bookedCount when booking created
    // ─────────────────────────────────────────────

    @Query("{ '_id': ?0 }")
    @Update("{ $inc: { 'bookedCount': 1 } }")
    void incrementBookedCount(String tourId);


    // ─────────────────────────────────────────────
    // US6 — Increment only if seats are still available
    // Returns modified document count (0 means full / not found)
    // ─────────────────────────────────────────────

    @Query("{ '_id': ?0, $expr: { $lt: ['$bookedCount', '$totalCapacity'] } }")
    @Update("{ $inc: { 'bookedCount': 1 } }")
    long incrementBookedCountIfCapacityAvailable(String tourId);


    // ─────────────────────────────────────────────
    // US6 — Decrement bookedCount when booking cancelled
    // ─────────────────────────────────────────────

    @Query("{ '_id': ?0 }")
    @Update("{ $inc: { 'bookedCount': -1 } }")
    void decrementBookedCount(String tourId);


    // ─────────────────────────────────────────────
    // US6 — Safe decrement for cancellation
    // Returns modified document count (0 means nothing decremented)
    // ─────────────────────────────────────────────

    @Query("{ '_id': ?0, 'bookedCount': { $gt: 0 } }")
    @Update("{ $inc: { 'bookedCount': -1 } }")
    long decrementBookedCountIfPositive(String tourId);

}