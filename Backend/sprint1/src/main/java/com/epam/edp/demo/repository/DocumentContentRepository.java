package com.epam.edp.demo.repository;

import com.epam.edp.demo.model.DocumentContent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DocumentContentRepository extends MongoRepository<DocumentContent, String> {
}
