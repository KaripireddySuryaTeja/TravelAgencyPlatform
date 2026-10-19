package com.epam.edp.demo.repository;

import com.epam.edp.demo.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByTourId(String tourId);

    List<Review> findByTourIdIn(Collection<String> tourIds);

    List<Review> findByTourIdAndHiddenFalse(String tourId);

    List<Review> findByHiddenTrue();
}
