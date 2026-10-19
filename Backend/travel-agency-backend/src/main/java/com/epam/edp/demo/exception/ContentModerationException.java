package com.epam.edp.demo.exception;

/**
 * Thrown when content moderation cannot be completed (service unavailable,
 * invalid response, auth failure, etc.) or when content requires user revision
 * ({@code NEEDS_EDIT}).
 *
 * <p>{@link #getModerationStatus()} returns a string token such as
 * {@code "NEEDS_EDIT"}, {@code "UNAVAILABLE"}, or {@code "INVALID_RESPONSE"}
 * that the {@code GlobalExceptionHandler} uses to tailor the HTTP response.</p>
 *
 * <p>Stack traces are never forwarded to the caller – only the
 * {@link #getUserMessage()} is safe for external consumption.</p>
 */
public class ContentModerationException extends RuntimeException {

    /** One of: NEEDS_EDIT, UNAVAILABLE, INVALID_RESPONSE, FLAGGED, ERROR */
    private final String moderationStatus;

    /** AI-generated or system-generated technical reason (for logging). */
    private final String technicalReason;

    /** User-friendly message – safe to include in HTTP responses. */
    private final String userMessage;

    public ContentModerationException(String moderationStatus,
                                      String technicalReason,
                                      String userMessage) {
        super(technicalReason);
        this.moderationStatus = moderationStatus;
        this.technicalReason  = technicalReason;
        this.userMessage      = userMessage;
    }

    public ContentModerationException(String moderationStatus,
                                      String technicalReason,
                                      String userMessage,
                                      Throwable cause) {
        super(technicalReason, cause);
        this.moderationStatus = moderationStatus;
        this.technicalReason  = technicalReason;
        this.userMessage      = userMessage;
    }

    public String getModerationStatus() {
        return moderationStatus;
    }

    public String getTechnicalReason() {
        return technicalReason;
    }

    public String getUserMessage() {
        return userMessage;
    }
}

