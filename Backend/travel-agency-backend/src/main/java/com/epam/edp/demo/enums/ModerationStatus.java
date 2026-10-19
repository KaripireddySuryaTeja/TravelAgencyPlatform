package com.epam.edp.demo.enums;

/**
 * Possible outcomes of the AI-powered content moderation step.
 *
 * <ul>
 *   <li>{@link #APPROVED}   – content is safe to publish as-is.</li>
 *   <li>{@link #NEEDS_EDIT} – content has fixable issues; user should revise and resubmit.</li>
 *   <li>{@link #FLAGGED}    – content violates community guidelines; submission is rejected.</li>
 * </ul>
 */
public enum ModerationStatus {
    APPROVED,
    NEEDS_EDIT,
    FLAGGED
}

