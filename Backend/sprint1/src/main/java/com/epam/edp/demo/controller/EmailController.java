package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.ChangeEmailRequestDTO;
import com.epam.edp.demo.dto.ConfirmEmailRequestDTO;
import com.epam.edp.demo.dto.MessageResponseDTO;
import com.epam.edp.demo.service.EmailService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller for email change operations.
 * Separated from UserController for better separation of concerns.
 */
@RestController
@RequestMapping("/api/v1/users")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * PUT /api/v1/users/{id}/email
     * Initiate email change - sends confirmation to new email.
     * The email is NOT changed until confirmed.
     */
    @PutMapping("/{id}/email")
    public ResponseEntity<MessageResponseDTO> changeEmail(
            @PathVariable String id,
            @Valid @RequestBody ChangeEmailRequestDTO request
    ) {
        String authenticatedUserId = getAuthenticatedUserId();
        emailService.initiateEmailChange(id, authenticatedUserId, request);
        return ResponseEntity.ok(MessageResponseDTO.of("A confirmation link has been sent to your new email address."));
    }

    /**
     * POST /api/v1/users/{id}/email/confirm
     * Confirm email change using the token sent to the new email.
     */
    @PostMapping("/{id}/email/confirm")
    public ResponseEntity<MessageResponseDTO> confirmEmailChange(
            @PathVariable String id,
            @Valid @RequestBody ConfirmEmailRequestDTO request
    ) {
        String authenticatedUserId = getAuthenticatedUserId();
        emailService.confirmEmailChange(id, authenticatedUserId, request);
        return ResponseEntity.ok(MessageResponseDTO.of("Your email has been successfully updated."));
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────
    private String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return auth.getName();
    }
}

