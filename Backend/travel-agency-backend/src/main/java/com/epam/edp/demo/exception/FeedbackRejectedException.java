package com.epam.edp.demo.exception;
/**
 * Thrown when user feedback is permanently rejected by the AI moderation system
 * because it violates community guidelines (status = {@code FLAGGED}).
 *
 * <p>The {@code reason} field contains the AI-provided explanation, which is safe
 * to surface to the user as a moderation rationale.</p>
 */
public class FeedbackRejectedException extends RuntimeException {
    private final String reason;
    public FeedbackRejectedException(String reason) {
        super("Your feedback was rejected: " + reason);
        this.reason = reason;
    }
    /**
     * @return The moderation reason produced by the AI model.
     */
    public String getReason() {
        return reason;
    }
}