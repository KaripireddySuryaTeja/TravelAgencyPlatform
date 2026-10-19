package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.ChangeEmailRequestDTO;
import com.epam.edp.demo.dto.ConfirmEmailRequestDTO;
import com.epam.edp.demo.exception.EmailAlreadyExistsException;
import com.epam.edp.demo.model.User;
import com.epam.edp.demo.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Service for handling email-related operations.
 * - Password reset emails (from develop branch)
 * - Email change verification (from feature branch)
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Duration emailTokenExpiry;

    @Value("${app.password-reset.from-email:#{null}}")
    private String fromEmail;

    @Value("${app.frontend.url:#{null}}")
    private String frontendUrl;

    public EmailService(
            JavaMailSender mailSender,
            UserRepository repository,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.email-token-expiry-hours:24}") long emailTokenExpiryHours
    ) {
        this.mailSender = mailSender;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.emailTokenExpiry = Duration.ofHours(emailTokenExpiryHours);
    }

    // ═══════════════════════════════════════════════════════════════
    // Password Reset Email (from develop branch)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Send password reset verification code email.
     */
    public void sendPasswordResetCode(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromEmail != null && !fromEmail.isEmpty()) {
                message.setFrom(fromEmail);
                log.debug("Using configured from email: {}", fromEmail);
            } else {
                log.warn("No from email configured. Email may fail or use default sender.");
            }
            message.setTo(to);
            message.setSubject("Password Reset Verification Code - Travel Agency");
            message.setText(buildPasswordResetEmailBody(code));
            
            log.debug("Attempting to send password reset email to: {}", to);
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}. Error: {} - {}", to, e.getClass().getSimpleName(), e.getMessage());
            if (e.getCause() != null) {
                log.error("Caused by: {} - {}", e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to send email. Please try again later. Error: " + e.getMessage());
        }
    }

    private String buildPasswordResetEmailBody(String code) {
        return String.format(
            "Hello,\n\n" +
            "You have requested to reset your password for your Travel Agency account.\n\n" +
            "Your verification code is: %s\n\n" +
            "This code will expire in 15 minutes.\n\n" +
            "If you did not request this password reset, please ignore this email.\n\n" +
            "Best regards,\n" +
            "Travel Agency Team",
            code
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // Email Verification for Registration
    // ═══════════════════════════════════════════════════════════════

    /**
     * Send email verification code for registration.
     */
    public void sendEmailVerificationCode(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromEmail != null && !fromEmail.isEmpty()) {
                message.setFrom(fromEmail);
                log.debug("Using configured from email: {}", fromEmail);
            } else {
                log.warn("No from email configured. Email may fail or use default sender.");
            }
            message.setTo(to);
            message.setSubject("Email Verification Code - Travel Agency");
            message.setText(buildEmailVerificationBody(code));
            
            log.debug("Attempting to send email verification to: {}", to);
            mailSender.send(message);
            log.info("Email verification sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email verification to: {}. Error: {} - {}", to, e.getClass().getSimpleName(), e.getMessage());
            if (e.getCause() != null) {
                log.error("Caused by: {} - {}", e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to send email. Please try again later. Error: " + e.getMessage());
        }
    }

    private String buildEmailVerificationBody(String code) {
        return String.format(
            "Hello,\n\n" +
            "Thank you for registering with Travel Agency!\n\n" +
            "Your verification code is: %s\n\n" +
            "This code will expire in 15 minutes.\n\n" +
            "Please enter this code to verify your email address and complete your registration.\n\n" +
            "If you did not create an account, please ignore this email.\n\n" +
            "Best regards,\n" +
            "Travel Agency Team",
            code
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // Email Change (from feature branch)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Initiate email change process - sends verification to new email.
     * The email is NOT changed until verified.
     */
    public String initiateEmailChange(String userId, String authenticatedUserId, ChangeEmailRequestDTO request) {
        validateUserAccess(userId, authenticatedUserId);
        User user = findUserOrThrow(userId);

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password is incorrect");
        }

        String newEmail = request.getNewEmail().trim().toLowerCase(Locale.ROOT);

        // Check if new email is same as current
        if (newEmail.equals(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New email is the same as current email");
        }

        // Check if new email is already in use
        if (repository.existsByEmail(newEmail)) {
            throw new EmailAlreadyExistsException(newEmail);
        }

        // Generate confirmation token
        String confirmationToken = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plus(emailTokenExpiry);

        user.setPendingEmail(newEmail);
        user.setEmailConfirmationToken(confirmationToken);
        user.setEmailConfirmationExpiry(expiry);
        repository.save(user);

        // Send confirmation email to new address
        sendEmailChangeConfirmation(newEmail, confirmationToken, userId, user.getFirstName());

        log.info("Email change initiated for user {}. New email: {}. Confirmation token: {}",
                userId, newEmail, confirmationToken);

        return confirmationToken;
    }

    /**
     * Send email change confirmation link to new email address.
     */
    private void sendEmailChangeConfirmation(String newEmail, String token, String userId, String firstName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromEmail != null && !fromEmail.isEmpty()) {
                message.setFrom(fromEmail);
            }
            message.setTo(newEmail);
            message.setSubject("Confirm Your Email Change - Travel Agency");
            message.setText(buildEmailChangeConfirmationBody(firstName, token, userId));
            
            mailSender.send(message);
            log.info("Email change confirmation sent successfully to: {}", newEmail);
        } catch (Exception e) {
            log.error("Failed to send email change confirmation to: {}", newEmail, e);
            throw new RuntimeException("Failed to send confirmation email. Please try again later.");
        }
    }

    private String buildEmailChangeConfirmationBody(String firstName, String token, String userId) {
        String baseUrl = frontendUrl != null ? frontendUrl : "http://localhost:3000";
        return String.format(
            "Hello %s,\n\n" +
            "You have requested to change your email address for your Travel Agency account.\n\n" +
            "Please use the following confirmation token to complete the change:\n\n" +
            "Token: %s\n\n" +
            "Or click the following link:\n" +
            "%s/confirm-email?token=%s&userId=%s\n\n" +
            "This token will expire in 24 hours.\n\n" +
            "If you did not request this email change, please ignore this email.\n\n" +
            "Best regards,\n" +
            "Travel Agency Team",
            firstName != null ? firstName : "User",
            token,
            baseUrl,
            token,
            userId
        );
    }

    /**
     * Confirm email change using the token sent to the new email.
     */
    public void confirmEmailChange(String userId, String authenticatedUserId, ConfirmEmailRequestDTO request) {
        validateUserAccess(userId, authenticatedUserId);
        User user = findUserOrThrow(userId);

        // Validate token
        if (user.getEmailConfirmationToken() == null ||
            !user.getEmailConfirmationToken().equals(request.getConfirmationToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired confirmation token");
        }

        // Check expiry
        if (user.getEmailConfirmationExpiry() == null ||
            Instant.now().isAfter(user.getEmailConfirmationExpiry())) {
            // Clear the pending email data
            user.setPendingEmail(null);
            user.setEmailConfirmationToken(null);
            user.setEmailConfirmationExpiry(null);
            repository.save(user);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirmation token has expired. Please request a new email change.");
        }

        // Check if pending email is still available
        String newEmail = user.getPendingEmail();
        if (newEmail == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending email change found");
        }

        if (repository.existsByEmail(newEmail) && !newEmail.equals(user.getEmail())) {
            throw new EmailAlreadyExistsException(newEmail);
        }

        // Update email
        String oldEmail = user.getEmail();
        user.setEmail(newEmail);
        user.setPendingEmail(null);
        user.setEmailConfirmationToken(null);
        user.setEmailConfirmationExpiry(null);
        repository.save(user);

        log.info("User {} changed email from {} to {}", userId, oldEmail, newEmail);
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
}

