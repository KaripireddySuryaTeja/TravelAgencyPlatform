package com.epam.edp.demo.service.moderation;

/**
 * Strategy interface for content moderation.
 *
 * <p>The production implementation ({@link ModerationServiceImpl}) delegates to
 * Azure OpenAI via function/tool calling.  A stub/mock can be substituted for
 * unit testing without a live API key.</p>
 */
public interface ModerationService {

    /**
     * Evaluate the supplied text and return a structured moderation decision.
     *
     * @param content The user-submitted feedback text to moderate.  May be {@code null}
     *                or blank; implementations should handle those cases gracefully.
     * @return A {@link ModerationResult} whose {@code status} is one of
     *         {@code APPROVED}, {@code NEEDS_EDIT}, or {@code FLAGGED}.
     * @throws com.epam.edp.demo.exception.ContentModerationException
     *         when the underlying AI service is unreachable or returns an
     *         invalid response.  Callers must NOT silently swallow this exception
     *         – content must not be published when moderation cannot be completed.
     */
    ModerationResult moderate(String content);
}

