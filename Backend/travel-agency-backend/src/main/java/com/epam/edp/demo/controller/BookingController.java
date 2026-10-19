package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.BookedTourListResponseDTO;
import com.epam.edp.demo.dto.ConfirmBookingChangesRequestDTO;
import com.epam.edp.demo.dto.CreateBookingRequestDTO;
import com.epam.edp.demo.dto.CreateBookingResponseDTO;
import com.epam.edp.demo.dto.UpdateBookingRequestDTO;
import com.epam.edp.demo.dto.ApiErrorResponseDTO;
import com.epam.edp.demo.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Booking management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private static final Pattern OBJECT_ID_PATTERN = Pattern.compile("^[a-fA-F0-9]{24}$");

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * POST /api/v1/bookings
     * Create a new booking — authenticated users only.
     */
    @PostMapping
        @Operation(summary = "Create booking", description = "Creates a booking for the authenticated user.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Booking created",
                content = @Content(schema = @Schema(implementation = CreateBookingResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class)))
        })
    public ResponseEntity<CreateBookingResponseDTO> createBooking(
                @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Booking payload. The userId must match the authenticated user.",
                    required = true)
            @Valid @RequestBody CreateBookingRequestDTO request
    ) {
        String userId = getAuthenticatedUserId();
        CreateBookingResponseDTO response = bookingService.createBooking(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/bookings?userId=...
     * GET /api/v1/bookings?agentId=...
     * Retrieve all bookings for a specific user OR all bookings for an agent's tours — authenticated users only.
     */
    @GetMapping
        @Operation(summary = "List bookings by user", description = "Returns bookings for the requested user when access rules allow it.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bookings retrieved",
                content = @Content(schema = @Schema(implementation = BookedTourListResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class)))
        })
    public ResponseEntity<BookedTourListResponseDTO> getBookings(
            @Parameter(description = "User identifier to query bookings for", example = "663f2a2c5c8f8d4f1ab12345")
            @RequestParam(required = false) String userId,
            @Parameter(description = "Travel agent identifier to query bookings for", example = "663f2a2c5c8f8d4f1ab12346")
            @RequestParam(required = false) String agentId
    ) {
        String authenticatedUserId = getAuthenticatedUserId();

        if (userId != null && !userId.isBlank()) {
            if (!OBJECT_ID_PATTERN.matcher(userId.trim()).matches()) {
                throw new IllegalArgumentException("Invalid userId format");
            }
            BookedTourListResponseDTO response = bookingService.getBookingsForUser(userId, authenticatedUserId);
            return ResponseEntity.ok(response);
        }

        if (agentId != null && !agentId.isBlank()) {
            BookedTourListResponseDTO response = bookingService.getBookingsForAgent(agentId.trim(), authenticatedUserId);
            return ResponseEntity.ok(response);
        }

        throw new IllegalArgumentException("Either userId or agentId query parameter is required");
    }

    /**
     * PATCH /api/v1/bookings/{id}/cancel
     * Cancel a booking — authenticated users only.
     */
    @PatchMapping("/{id}/cancel")
        @Operation(summary = "Cancel booking", description = "Cancels an existing booking for the authenticated user.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking canceled",
                content = @Content(schema = @Schema(implementation = Object.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class)))
        })
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @Parameter(description = "Booking identifier", required = true, example = "6641b2ea5c8f8d4f1ab67890")
            @PathVariable String id,
            @Parameter(description = "Optional reason for cancellation", example = "Change of travel plans")
            @RequestParam(required = false) String cancelReason,
            @RequestBody(required = false) Map<String, String> requestBody
    ) {
        String authenticatedUserId = getAuthenticatedUserId();
        // Support cancelReason from query param OR request body
        String reason = cancelReason;
        if (reason == null && requestBody != null && requestBody.containsKey("cancelReason")) {
            reason = requestBody.get("cancelReason");
        }
        LocalDate freeCancelDeadline = bookingService.cancelBooking(id, authenticatedUserId, reason);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("message", "Booking cancelled successfully");
        if (freeCancelDeadline != null) {
            body.put("freeCancelation", freeCancelDeadline.toString());
        }
        return ResponseEntity.ok(body);
    }

    /**
     * PUT /api/v1/bookings/{id}
     * Update a booking — authenticated users only.
     */
    @PutMapping("/{id}")
        @Operation(summary = "Update booking", description = "Updates an existing booking for the authenticated user.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking updated",
                content = @Content(schema = @Schema(implementation = Object.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class)))
        })
    public ResponseEntity<Map<String, Object>> updateBooking(
            @Parameter(description = "Booking identifier", required = true, example = "6641b2ea5c8f8d4f1ab67890")
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Editable booking fields. Include only fields that need to change.",
                required = true)
            @Valid @RequestBody UpdateBookingRequestDTO request
    ) {
        String authenticatedUserId = getAuthenticatedUserId();
        Map<String, Object> response = bookingService.updateBooking(id, authenticatedUserId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm-changes")
    public ResponseEntity<Map<String, Object>> confirmBookingChanges(
            @PathVariable String id,
            @RequestBody(required = false) ConfirmBookingChangesRequestDTO request
    ) {
        String authenticatedUserId = getAuthenticatedUserId();
        Map<String, Object> response = bookingService.confirmBookingChanges(id, authenticatedUserId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/v1/bookings/{id}/verify-documents
     * Travel Agent verifies all documents for a booking.
     */
    @PatchMapping("/{id}/verify-documents")
    @Operation(summary = "Verify documents", description = "Travel Agent marks all customer documents as verified.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Documents verified"),
        @ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Booking not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<Map<String, Object>> verifyDocuments(
            @PathVariable String id
    ) {
        String authenticatedUserId = getAuthenticatedUserId();
        Map<String, Object> response = bookingService.verifyDocuments(id, authenticatedUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/v1/bookings/{id}/confirm
     * Travel Agent confirms a booking after document verification.
     */
    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirm booking", description = "Travel Agent confirms the booking after checking documents.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking confirmed"),
        @ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Booking not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<Map<String, Object>> confirmBooking(
            @PathVariable String id
    ) {
        String authenticatedUserId = getAuthenticatedUserId();
        Map<String, Object> response = bookingService.confirmBooking(id, authenticatedUserId);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────
    // Helper: extract userId from JWT context
    // ─────────────────────────────────────────────
    private String getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return auth.getName();
    }
}

