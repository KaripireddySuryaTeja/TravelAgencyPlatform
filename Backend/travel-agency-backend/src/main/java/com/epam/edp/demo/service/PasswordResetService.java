package com.epam.edp.demo.service;

import com.epam.edp.demo.model.PasswordResetToken;
import com.epam.edp.demo.model.User;
import com.epam.edp.demo.repository.PasswordResetTokenRepository;
import com.epam.edp.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom random = new SecureRandom();
    private static final int CODE_LENGTH = 6;

    @Transactional
    public void requestPasswordReset(String email) {
        // Check if user exists - silently return if not found (security: don't reveal if email exists)
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return; // Don't reveal that user doesn't exist
        }
        User user = userOpt.get();

        // Delete any existing reset tokens for this email
        resetTokenRepository.deleteByEmail(email);

        // Generate 6-digit verification code
        String code = generateVerificationCode();

        // Create and save new reset token
        PasswordResetToken resetToken = new PasswordResetToken(email, code);
        resetTokenRepository.save(resetToken);

        // Send email
        emailService.sendPasswordResetCode(email, code);

        log.info("Password reset requested for email: {}", email);
    }

    @Transactional
    public boolean verifyResetCode(String email, String code) {
        PasswordResetToken token = resetTokenRepository
                .findByEmailAndCodeAndUsedFalse(email, code)
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification code"));

        if (!token.isValid()) {
            throw new RuntimeException("Verification code has expired");
        }

        log.info("Verification code validated for email: {}", email);
        return true;
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        // Verify the code
        PasswordResetToken token = resetTokenRepository
                .findByEmailAndCodeAndUsedFalse(email, code)
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification code"));

        if (!token.isValid()) {
            throw new RuntimeException("Verification code has expired");
        }

        // Get user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        token.setUsed(true);
        resetTokenRepository.save(token);

        log.info("Password reset successfully for email: {}", email);
    }

    private String generateVerificationCode() {
        int code = random.nextInt(900000) + 100000; // Generate 6-digit code
        return String.valueOf(code);
    }
}
