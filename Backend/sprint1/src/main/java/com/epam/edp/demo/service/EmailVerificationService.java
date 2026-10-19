package com.epam.edp.demo.service;

import com.epam.edp.demo.model.EmailVerificationToken;
import com.epam.edp.demo.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * Service for email verification during user registration.
 * Generates a 6-digit OTP, stores it with a 15-minute TTL, and sends it via email.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;

    private static final SecureRandom random = new SecureRandom();
    private static final int CODE_LENGTH = 6;

    @Transactional
    public void requestEmailVerification(String email) {
        // Delete any existing verification tokens for this email
        verificationTokenRepository.deleteByEmail(email);

        // Generate 6-digit verification code
        String code = generateVerificationCode();

        // Create and save new verification token
        EmailVerificationToken verificationToken = new EmailVerificationToken(email, code);
        verificationTokenRepository.save(verificationToken);

        // Send email
        emailService.sendEmailVerificationCode(email, code);

        log.info("Email verification requested for email: {}", email);
    }

    @Transactional
    public boolean verifyEmailCode(String email, String code) {
        EmailVerificationToken token = verificationTokenRepository
                .findByEmailAndCodeAndUsedFalse(email, code)
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification code"));

        if (!token.isValid()) {
            throw new RuntimeException("Verification code has expired");
        }

        // Mark token as used
        token.setUsed(true);
        verificationTokenRepository.save(token);

        log.info("Email verification code validated for email: {}", email);
        return true;
    }

    private String generateVerificationCode() {
        int code = random.nextInt(900000) + 100000; // Generate 6-digit code
        return String.valueOf(code);
    }
}
