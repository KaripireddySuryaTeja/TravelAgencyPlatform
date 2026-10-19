package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.MessageResponseDTO;
import com.epam.edp.demo.dto.UpdatePasswordRequestDTO;
import com.epam.edp.demo.dto.UpdateUserImageRequestDTO;
import com.epam.edp.demo.dto.UpdateUserNameRequestDTO;
import com.epam.edp.demo.dto.UserDTO;
import com.epam.edp.demo.service.UserService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller for user profile management (User Story 12).
 * Provides endpoints for viewing and updating user profile information.
 * Note: Email change endpoints are handled by EmailController.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/v1/users/{id}
     * Get user profile information.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable String id) {
        String authenticatedUserId = getAuthenticatedUserId();
        UserDTO user = userService.getUserById(id, authenticatedUserId);
        return ResponseEntity.ok(user);
    }

    /**
     * PUT /api/v1/users/{id}/name
     * Update user's first and last name.
     */
    @PutMapping("/{id}/name")
    public ResponseEntity<MessageResponseDTO> updateUserName(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserNameRequestDTO request
    ) {
        String authenticatedUserId = getAuthenticatedUserId();
        userService.updateUserName(id, authenticatedUserId, request);
        return ResponseEntity.ok(MessageResponseDTO.of("Your account has been updated successfully"));
    }

    /**
     * PUT /api/v1/users/{id}/image
     * Upload/update user's profile image (avatar) as Base64.
     */
    @PutMapping("/{id}/image")
    public ResponseEntity<MessageResponseDTO> updateUserImage(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserImageRequestDTO request
    ) {
        String authenticatedUserId = getAuthenticatedUserId();
        userService.updateUserImage(id, authenticatedUserId, request);
        return ResponseEntity.ok(MessageResponseDTO.of("Your account has been updated successfully"));
    }

    /**
     * PUT /api/v1/users/{id}/password
     * Change user's password.
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<MessageResponseDTO> updatePassword(
            @PathVariable String id,
            @Valid @RequestBody UpdatePasswordRequestDTO request
    ) {
        String authenticatedUserId = getAuthenticatedUserId();
        userService.updatePassword(id, authenticatedUserId, request);
        return ResponseEntity.ok(MessageResponseDTO.of("Your password has been updated successfully"));
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

