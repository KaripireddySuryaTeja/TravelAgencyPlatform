package com.epam.edp.demo.service;

import com.epam.edp.demo.model.EmailVerificationToken;
import com.epam.edp.demo.repository.EmailVerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository verificationTokenRepository;

    @Mock
    private EmailService emailService;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationService(verificationTokenRepository, emailService);
    }

    // ─── requestEmailVerification ─────────────────────────────────────────────

    @Test
    void requestEmailVerification_deletesExistingTokensForEmail() {
        emailVerificationService.requestEmailVerification("user@example.com");

        verify(verificationTokenRepository).deleteByEmail("user@example.com");
    }

    @Test
    void requestEmailVerification_savesNewToken() {
        emailVerificationService.requestEmailVerification("user@example.com");

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(verificationTokenRepository).save(captor.capture());
        assertEquals("user@example.com", captor.getValue().getEmail());
    }

    @Test
    void requestEmailVerification_savedTokenCodeIsSixDigits() {
        emailVerificationService.requestEmailVerification("user@example.com");

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(verificationTokenRepository).save(captor.capture());
        String code = captor.getValue().getCode();
        assertTrue(code.matches("\\d{6}"), "Code should be 6 digits but was: " + code);
    }

    @Test
    void requestEmailVerification_sendsEmailWithCode() {
        emailVerificationService.requestEmailVerification("user@example.com");

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(verificationTokenRepository).save(captor.capture());
        String code = captor.getValue().getCode();

        verify(emailService).sendEmailVerificationCode("user@example.com", code);
    }

    @Test
    void requestEmailVerification_savedTokenIsNotUsed() {
        emailVerificationService.requestEmailVerification("user@example.com");

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(verificationTokenRepository).save(captor.capture());
        assertTrue(!captor.getValue().isUsed());
    }

    // ─── verifyEmailCode ──────────────────────────────────────────────────────

    @Test
    void verifyEmailCode_returnsTrueForValidCode() {
        EmailVerificationToken token = validToken("user@example.com", "123456");
        when(verificationTokenRepository.findByEmailAndCodeAndUsedFalse("user@example.com", "123456"))
                .thenReturn(Optional.of(token));

        boolean result = emailVerificationService.verifyEmailCode("user@example.com", "123456");

        assertTrue(result);
    }

    @Test
    void verifyEmailCode_marksTokenAsUsed() {
        EmailVerificationToken token = validToken("user@example.com", "123456");
        when(verificationTokenRepository.findByEmailAndCodeAndUsedFalse("user@example.com", "123456"))
                .thenReturn(Optional.of(token));

        emailVerificationService.verifyEmailCode("user@example.com", "123456");

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(verificationTokenRepository).save(captor.capture());
        assertTrue(captor.getValue().isUsed());
    }

    @Test
    void verifyEmailCode_throwsWhenTokenNotFound() {
        when(verificationTokenRepository.findByEmailAndCodeAndUsedFalse(anyString(), anyString()))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailVerificationService.verifyEmailCode("user@example.com", "000000"));

        assertEquals("Invalid or expired verification code", ex.getMessage());
    }

    @Test
    void verifyEmailCode_throwsWhenTokenExpired() {
        EmailVerificationToken token = expiredToken("user@example.com", "123456");
        when(verificationTokenRepository.findByEmailAndCodeAndUsedFalse("user@example.com", "123456"))
                .thenReturn(Optional.of(token));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailVerificationService.verifyEmailCode("user@example.com", "123456"));

        assertEquals("Verification code has expired", ex.getMessage());
    }

    @Test
    void verifyEmailCode_doesNotSaveWhenTokenExpired() {
        EmailVerificationToken token = expiredToken("user@example.com", "123456");
        when(verificationTokenRepository.findByEmailAndCodeAndUsedFalse("user@example.com", "123456"))
                .thenReturn(Optional.of(token));

        assertThrows(RuntimeException.class,
                () -> emailVerificationService.verifyEmailCode("user@example.com", "123456"));

        verify(verificationTokenRepository, never()).save(any());
    }

    @Test
    void verifyEmailCode_doesNotSaveWhenTokenNotFound() {
        when(verificationTokenRepository.findByEmailAndCodeAndUsedFalse(anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> emailVerificationService.verifyEmailCode("user@example.com", "000000"));

        verify(verificationTokenRepository, never()).save(any());
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private EmailVerificationToken validToken(String email, String code) {
        EmailVerificationToken token = new EmailVerificationToken(email, code);
        // expiresAt is 15 minutes from now by default — still valid
        return token;
    }

    private EmailVerificationToken expiredToken(String email, String code) {
        EmailVerificationToken token = new EmailVerificationToken(email, code);
        token.setExpiresAt(Instant.now().minusSeconds(1)); // already expired
        return token;
    }
}
