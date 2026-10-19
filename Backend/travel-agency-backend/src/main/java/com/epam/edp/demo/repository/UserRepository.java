package com.epam.edp.demo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.epam.edp.demo.enums.AuthProvider;
import com.epam.edp.demo.enums.Role;
import com.epam.edp.demo.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    Optional<User> findByEmailConfirmationToken(String token);
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}

