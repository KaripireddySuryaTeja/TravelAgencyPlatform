package com.epam.edp.demo.service.moderation;

import com.azure.ai.openai.OpenAIClient;
import com.azure.core.exception.HttpResponseException;
import com.epam.edp.demo.enums.ModerationStatus;
import com.epam.edp.demo.exception.ContentModerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Production {@link ModerationService} implementation.
 *
 * <p>Delegates the raw Azure OpenAI interaction to {@link AiModerationService},
 * which keeps the SDK ceremony isolated.  This class is responsible for:
 * <ul>
 *   <li>Null / blank content fast-path (auto-APPROVED).</li>
 *   <li>Translating every possible SDK or parsing failure into a
 *       {@link ContentModerationException} so callers never see raw stack
 *       traces and content is never published when moderation fails.</li>
 *   <li>Structured, detailed logging for operations and diagnostics.</li>
 * </ul>
 *
 * <p>Security: the API key is injected from the environment variable
 * {@code AZURE_OPENAI_API_KEY} – see {@code AzureOpenAiConfiguration}.
 * No secrets are hardcoded.</p>
 */
@Service
public class ModerationServiceImpl implements ModerationService {

    private static final Logger log = LoggerFactory.getLogger(ModerationServiceImpl.class);

    private final OpenAIClient openAIClient;
    private final String deploymentName;
    private final AiModerationService aiModerationService;

    public ModerationServiceImpl(
            OpenAIClient openAIClient,
            @Value("${app.moderation.ai.deployment:gpt-4.1-mini-2025-04-14}") String deploymentName) {
        this.openAIClient = openAIClient;
        this.deploymentName = deploymentName;
        this.aiModerationService = new AiModerationService();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Blank or {@code null} content is automatically {@code APPROVED} –
     * comment validation already enforces the rating/comment business rules.</p>
     *
     * @throws ContentModerationException when the service is unavailable,
     *         credentials are invalid, or the AI response cannot be parsed.
     *         Content is never published in these cases.
     */
    @Override
    public ModerationResult moderate(String content) {
        // Empty comments are allowed by business rules (rating >= 4); skip AI call.
        if (content == null || content.isBlank()) {
            log.debug("Blank/null comment – skipping AI moderation, returning APPROVED.");
            return new ModerationResult(ModerationStatus.APPROVED,
                    "Empty content does not require moderation.");
        }

        try {
            log.info("Submitting content for AI moderation (chars={})", content.length());
            ModerationResult result = aiModerationService.callModeration(
                    openAIClient, deploymentName, content);
            log.info("AI moderation completed: status={}, reason='{}'",
                    result.status(), result.reason());
            return result;

        } catch (HttpResponseException e) {
            int statusCode = e.getResponse().getStatusCode();
            log.error("Azure OpenAI HTTP {} during moderation: {}", statusCode, e.getMessage());

            if (statusCode == 401 || statusCode == 403) {
                throw new ContentModerationException(
                        "FLAGGED",
                        "Moderation service authentication failed.",
                        "Content moderation is unavailable due to an authentication issue. "
                                + "Please contact support.",
                        e);
            }
            if (statusCode == 429) {
                throw new ContentModerationException(
                        "UNAVAILABLE",
                        "Moderation service rate limit exceeded.",
                        "Content moderation is temporarily unavailable (rate limit). "
                                + "Please try again in a few moments.",
                        e);
            }
            throw new ContentModerationException(
                    "UNAVAILABLE",
                    "Moderation service returned HTTP " + statusCode + ".",
                    "Content moderation is temporarily unavailable. Please try again later.",
                    e);

        } catch (IllegalStateException e) {
            log.error("Invalid response from AI moderation service: {}", e.getMessage(), e);
            throw new ContentModerationException(
                    "INVALID_RESPONSE",
                    "AI moderation returned an invalid or unparseable response.",
                    "Content moderation could not be completed. Please try again.",
                    e);

        } catch (Exception e) {
            log.error("Unexpected error during AI content moderation", e);
            throw new ContentModerationException(
                    "ERROR",
                    "Unexpected moderation error: " + e.getMessage(),
                    "Content moderation failed unexpectedly. Please try again later.",
                    e);
        }
    }
}

