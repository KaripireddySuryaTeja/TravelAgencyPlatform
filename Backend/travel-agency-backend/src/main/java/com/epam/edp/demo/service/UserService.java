package com.epam.edp.demo.service;


import com.epam.edp.demo.dto.SignInRequestDTO;
import com.epam.edp.demo.dto.SignInResponseDTO;
import com.epam.edp.demo.dto.SignUpRequestDTO;
import com.epam.edp.demo.dto.UpdatePasswordRequestDTO;
import com.epam.edp.demo.dto.UpdateUserImageRequestDTO;
import com.epam.edp.demo.dto.UpdateUserNameRequestDTO;
import com.epam.edp.demo.dto.UserDTO;
import com.epam.edp.demo.enums.Role;
import com.epam.edp.demo.exception.AccountLockedException;
import com.epam.edp.demo.exception.EmailAlreadyExistsException;
import com.epam.edp.demo.exception.InvalidCredentialsException;
import com.epam.edp.demo.exception.WeakPasswordException;
import com.epam.edp.demo.model.User;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.security.JwtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final int maxFailedAttempts;
    private final Duration lockoutDuration;

    public UserService(
            UserRepository repository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.auth.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${app.auth.lockout-minutes:15}") long lockoutMinutes
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutDuration = Duration.ofMinutes(lockoutMinutes);
    }

    public boolean emailExists(String email) {
        return repository.existsByEmail(email);
    }

    public User signUp(SignUpRequestDTO req) {
        String firstName = req.getFirstName().trim();
        String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
        String password = req.getPassword();

        rejectPasswordContainingIdentity(password, firstName, email);

        if (repository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        String lastName = req.getLastName().trim();
        User user = new User(firstName, lastName, email, passwordEncoder.encode(password), Role.CUSTOMER);
        try {
            return repository.save(user);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new EmailAlreadyExistsException(email, e);
        }
    }

    public SignInResponseDTO signIn(SignInRequestDTO req) {
        String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
        String password = req.getPassword();

        User user = repository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        Instant now = Instant.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long retryAfter = Duration.between(now, user.getLockedUntil()).toSeconds();
            throw new AccountLockedException(Math.max(retryAfter, 1));
        }
        if (user.getLockedUntil() != null) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= maxFailedAttempts) {
                Instant lockUntil = now.plus(lockoutDuration);
                user.setLockedUntil(lockUntil);
                repository.save(user);
                throw new AccountLockedException(lockoutDuration.toSeconds());
            }
            repository.save(user);
            throw new InvalidCredentialsException();
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        User saved = repository.save(user);

        JwtService.IssuedToken issued = jwtService.issue(saved.getId(), saved.getEmail(), saved.getFirstName(), saved.getRole().name());
        String userName = (saved.getFirstName() + " " + saved.getLastName()).trim();
        return new SignInResponseDTO(issued.token(), saved.getRole(), userName, saved.getEmail(), issued.expiresAt());
    }

    @SuppressWarnings("null")
    public User requireById(String userId) {
        return repository.findById(userId).orElseThrow(InvalidCredentialsException::new);
    }

    // ─────────────────────────────────────────────
    // Profile Management Methods (User Story 12)
    // ─────────────────────────────────────────────

    /**
     * Get user profile information by ID.
     */
    public UserDTO getUserById(String userId, String authenticatedUserId) {
        validateUserAccess(userId, authenticatedUserId);
        User user = findUserOrThrow(userId);
        return UserDTO.from(user);
    }

    /**
     * Update user's first and last name.
     */
    public void updateUserName(String userId, String authenticatedUserId, UpdateUserNameRequestDTO request) {
        validateUserAccess(userId, authenticatedUserId);
        User user = findUserOrThrow(userId);

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        repository.save(user);

        log.info("User {} updated their name to {} {}", userId, user.getFirstName(), user.getLastName());
    }

    /**
     * Update user's profile image (avatar) using Base64 encoded string.
     */
    public void updateUserImage(String userId, String authenticatedUserId, UpdateUserImageRequestDTO request) {
        validateUserAccess(userId, authenticatedUserId);
        User user = findUserOrThrow(userId);

        // Store as data URL for direct use in img src
        String imageBase64 = request.getImageBase64();
        if (!imageBase64.startsWith("data:image/")) {
            // Assume it's raw base64, prepend a generic data URL prefix
            imageBase64 = "data:image/png;base64," + imageBase64;
        }

        user.setImageUrl(imageBase64);
        repository.save(user);

        log.info("User {} updated their profile image", userId);
    }

    /**
     * Update user's password after verifying current password.
     */
    public void updatePassword(String userId, String authenticatedUserId, UpdatePasswordRequestDTO request) {
        validateUserAccess(userId, authenticatedUserId);
        User user = findUserOrThrow(userId);

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

        // Check that new password doesn't contain user's name or email
        rejectPasswordContainingIdentity(request.getNewPassword(), user.getFirstName(), user.getEmail());

        // Ensure new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);

        log.info("User {} changed their password", userId);
    }


    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    private void validateUserAccess(String requestedUserId, String authenticatedUserId) {
        if (!requestedUserId.equals(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own profile");
        }
    }

    private User findUserOrThrow(String userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    private void rejectPasswordContainingIdentity(String password, String firstName, String email) {
        String pw = password.toLowerCase(Locale.ROOT);
        if (!firstName.isBlank() && pw.contains(firstName.toLowerCase(Locale.ROOT))) {
            throw new WeakPasswordException("Password must not contain your first name");
        }
        String localPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        if (!localPart.isBlank() && pw.contains(localPart)) {
            throw new WeakPasswordException("Password must not contain your email");
        }
    }
}

