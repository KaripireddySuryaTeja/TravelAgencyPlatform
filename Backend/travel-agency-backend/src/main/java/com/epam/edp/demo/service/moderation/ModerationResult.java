package com.epam.edp.demo.service.moderation;

import com.epam.edp.demo.enums.ModerationStatus;

/**
 * Immutable result produced by the AI content moderation service.
 *
 * @param status {@link ModerationStatus} – moderation decision.
 * @param reason Human-readable explanation for the decision provided by the AI model.
 */
public record ModerationResult(
        ModerationStatus status,
        String reason) {
}

