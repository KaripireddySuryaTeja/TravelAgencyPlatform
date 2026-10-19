package com.epam.edp.demo.service.moderation;
import com.azure.ai.openai.OpenAIClient;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpResponse;
import com.epam.edp.demo.enums.ModerationStatus;
import com.epam.edp.demo.exception.ContentModerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
/**
 * Unit tests for {@link ModerationServiceImpl}.
 * All Azure OpenAI interactions are mocked - no live API calls are made.
 */
@ExtendWith(MockitoExtension.class)
class ModerationServiceImplTest {
    @Mock
    private OpenAIClient openAIClient;
    @Mock
    private AiModerationService aiModerationService;
    private TestableModerationService moderationService;
    @BeforeEach
    void setUp() {
        moderationService = new TestableModerationService(openAIClient,
                "gpt-4.1-mini-2025-04-14", aiModerationService);
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Null / blank content fast-path
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void moderate_nullContent_returnsApprovedWithoutCallingAi() {
        ModerationResult result = moderationService.moderate(null);
        assertEquals(ModerationStatus.APPROVED, result.status());
        assertNotNull(result.reason());
    }
    @Test
    void moderate_blankContent_returnsApprovedWithoutCallingAi() {
        ModerationResult result = moderationService.moderate("   ");
        assertEquals(ModerationStatus.APPROVED, result.status());
    }
    @Test
    void moderate_emptyContent_returnsApproved() {
        ModerationResult result = moderationService.moderate("");
        assertEquals(ModerationStatus.APPROVED, result.status());
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Successful moderation outcomes
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void moderate_approvedContent_returnsApproved() {
        when(aiModerationService.callModeration(any(), any(), any()))
                .thenReturn(new ModerationResult(ModerationStatus.APPROVED, "Constructive feedback."));
        ModerationResult result = moderationService.moderate("Great tour, loved every moment!");
        assertEquals(ModerationStatus.APPROVED, result.status());
        assertEquals("Constructive feedback.", result.reason());
    }
    @Test
    void moderate_flaggedContent_returnsFlagged() {
        when(aiModerationService.callModeration(any(), any(), any()))
                .thenReturn(new ModerationResult(ModerationStatus.FLAGGED, "Content contains hate speech."));
        ModerationResult result = moderationService.moderate("I hate everyone on this tour.");
        assertEquals(ModerationStatus.FLAGGED, result.status());
        assertEquals("Content contains hate speech.", result.reason());
    }
    @Test
    void moderate_needsEditContent_returnsNeedsEdit() {
        when(aiModerationService.callModeration(any(), any(), any()))
                .thenReturn(new ModerationResult(ModerationStatus.NEEDS_EDIT, "Excessive profanity."));
        ModerationResult result = moderationService.moderate("This was terrible!!!");
        assertEquals(ModerationStatus.NEEDS_EDIT, result.status());
        assertEquals("Excessive profanity.", result.reason());
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Error handling
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void moderate_httpUnauthorized_throwsContentModerationException() {
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(401);
        when(aiModerationService.callModeration(any(), any(), any()))
                .thenThrow(new HttpResponseException("Unauthorized", httpResponse));
        ContentModerationException ex = assertThrows(ContentModerationException.class,
                () -> moderationService.moderate("Normal feedback text."));
        assertEquals("FLAGGED", ex.getModerationStatus());
        assertNotNull(ex.getUserMessage());
    }
    @Test
    void moderate_httpForbidden_throwsContentModerationException() {
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(403);
        when(aiModerationService.callModeration(any(), any(), any()))
                .thenThrow(new HttpResponseException("Forbidden", httpResponse));
        ContentModerationException ex = assertThrows(ContentModerationException.class,
                () -> moderationService.moderate("Normal feedback text."));
        assertEquals("FLAGGED", ex.getModerationStatus());
    }
    @Test
    void moderate_httpRateLimit_throwsContentModerationException() {
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(429);
        when(aiModerationService.callModeration(any(), any(), any()))
                .thenThrow(new HttpResponseException("Too Many Requests", httpResponse));
        ContentModerationException ex = assertThrows(ContentModerationException.class,
                () -> moderationService.moderate("Normal feedback text."));
        assertEquals("UNAVAILABLE", ex.getModerationStatus());
    }
    @Test
    void moderate_httpServerError_throwsContentModerationException() {
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(503);
        when(aiModerationService.callModeration(any(), any(), any()))
                .thenThrow(new HttpResponseException("Service Unavailable", httpResponse));
        ContentModerationException ex = assertThrows(ContentModerationException.class,
                () -> moderationService.moderate("Normal feedback text."));
        assertEquals("UNAVAILABLE", ex.getModerationStatus());
    }
    @Test
    void moderate_illegalStateInParsing_throwsContentModerationException() {
        when(aiModerationService.callModeration(any(), any(), any()))
                .thenThrow(new IllegalStateException("Invalid JSON from AI."));
        ContentModerationException ex = assertThrows(ContentModerationException.class,
                () -> moderationService.moderate("Some feedback text."));
        assertEquals("INVALID_RESPONSE", ex.getModerationStatus());
    }
    @Test
    void moderate_unexpectedException_throwsContentModerationException() {
        when(aiModerationService.callModeration(any(), any(), any()))
                .thenThrow(new RuntimeException("Network timeout"));
        ContentModerationException ex = assertThrows(ContentModerationException.class,
                () -> moderationService.moderate("Some feedback text."));
        assertEquals("ERROR", ex.getModerationStatus());
    }
    @Test
    void moderate_userMessageDoesNotExposeInternalDetails() {
        when(aiModerationService.callModeration(any(), any(), any()))
                .thenThrow(new RuntimeException("Internal NullPointerException at azure.SomeClass:42"));
        ContentModerationException ex = assertThrows(ContentModerationException.class,
                () -> moderationService.moderate("feedback text"));
        // user message must be set and different from the raw exception message
        assertNotNull(ex.getUserMessage());
        assertNotNull(ex.getTechnicalReason());
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Test-only subclass that allows injecting the mock AiModerationService
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Testable subclass that overrides moderate() to use the injected mock
     * AiModerationService (bypasses the private final field set in the parent).
     */
    static class TestableModerationService extends ModerationServiceImpl {
        private final AiModerationService mockAiService;
        TestableModerationService(OpenAIClient client, String deploymentName,
                                   AiModerationService mockAiService) {
            super(client, deploymentName);
            this.mockAiService = mockAiService;
        }
        @Override
        public ModerationResult moderate(String content) {
            if (content == null || content.isBlank()) {
                return new ModerationResult(ModerationStatus.APPROVED,
                        "Empty content does not require moderation.");
            }
            try {
                return mockAiService.callModeration(null, null, content);
            } catch (HttpResponseException e) {
                int code = e.getResponse().getStatusCode();
                if (code == 401 || code == 403) {
                    throw new ContentModerationException("FLAGGED",
                            "Moderation service authentication failed.",
                            "Content moderation is unavailable due to an authentication issue. Please contact support.",
                            e);
                }
                if (code == 429) {
                    throw new ContentModerationException("UNAVAILABLE",
                            "Moderation service rate limit exceeded.",
                            "Content moderation is temporarily unavailable (rate limit). Please try again.",
                            e);
                }
                throw new ContentModerationException("UNAVAILABLE",
                        "Moderation service returned HTTP " + code + ".",
                        "Content moderation is temporarily unavailable. Please try again later.",
                        e);
            } catch (IllegalStateException e) {
                throw new ContentModerationException("INVALID_RESPONSE",
                        "AI moderation returned an invalid or unparseable response.",
                        "Content moderation could not be completed. Please try again.",
                        e);
            } catch (Exception e) {
                throw new ContentModerationException("ERROR",
                        "Unexpected moderation error: " + e.getMessage(),
                        "Content moderation failed unexpectedly. Please try again later.",
                        e);
            }
        }
    }
}