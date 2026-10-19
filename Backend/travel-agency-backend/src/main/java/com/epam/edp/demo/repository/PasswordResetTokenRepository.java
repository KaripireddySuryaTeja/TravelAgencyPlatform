package com.epam.edp.demo.repository;

import com.epam.edp.demo.model.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByEmailAndCodeAndUsedFalse(String email, String code);
    void deleteByEmail(String email);
}
