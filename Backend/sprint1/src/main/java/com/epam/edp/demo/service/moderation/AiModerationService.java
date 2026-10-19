package com.epam.edp.demo.service.moderation;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatCompletionsToolCall;
import com.azure.ai.openai.models.ChatCompletionsToolDefinition;
import com.azure.ai.openai.models.ChatCompletionsToolSelection;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.openai.models.ChatCompletionsFunctionToolDefinition;
import com.azure.ai.openai.models.ChatCompletionsFunctionToolDefinitionFunction;
import com.azure.ai.openai.models.ChatCompletionsFunctionToolCall;
import com.azure.ai.openai.models.ChatCompletionsFunctionToolSelection;
import com.azure.ai.openai.models.ChatCompletionsNamedFunctionToolSelection;
import com.azure.core.util.BinaryData;
import com.epam.edp.demo.enums.ModerationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Low-level helper that performs the raw Azure OpenAI function/tool-calling
 * interaction for content moderation.
 *
 * <p>This class is intentionally <em>not</em> a Spring bean.  It is instantiated
 * by {@link ModerationServiceImpl}, which owns the lifecycle and provides all
 * error handling.  Keeping the Azure SDK calls in a separate, package-private
 * class makes mocking straightforward in unit tests.</p>
 */
class AiModerationService {

    private static final Logger log = LoggerFactory.getLogger(AiModerationService.class);

    static final String TOOL_NAME = "submit_moderation";

    /**
     * System prompt that instructs the model to act as a content moderator
     * and to always call {@code submit_moderation}.
     */
    private static final String SYSTEM_PROMPT = """
            You are a content moderation system for a travel agency feedback platform.
            Your sole responsibility is to evaluate user-submitted feedback text and call
            the submit_moderation function with the appropriate decision.

            Decision rules:

            APPROVED – Allow the content when it:
              • Is constructive and provides criticism respectfully.
              • Uses mild profanity only for emphasis (e.g., "this was damn expensive").
              • Contains minor spelling or grammar errors.
              • Discusses controversial topics in a respectful, non-harmful manner.

            NEEDS_EDIT – Request revision when the content:
              • Contains excessive or repeated profanity.
              • Is so poorly written that it is largely incomprehensible.
              • Can be meaningfully improved and resubmitted.

            FLAGGED – Reject the content immediately when it contains:
              • Hate speech, harassment, or discrimination based on any characteristic.
              • Threats of violence or harm toward any individual or group.
              • Explicit sexual content.
              • Doxxing or sharing of private personal information.
              • Promotion of illegal activities.
              • Aggressive, targeted abusive language.
              • Spam, advertisements, or off-topic promotional content.

            Always provide a concise, clear reason for your decision.
            Never reject good-faith constructive criticism.
            Apply the rules consistently and transparently.
            You MUST call submit_moderation.
            """;

    /** JSON Schema for the submit_moderation tool parameters. */
    private static final String TOOL_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "status": {
                  "type": "string",
                  "enum": ["APPROVED", "FLAGGED", "NEEDS_EDIT"],
                  "description": "The moderation decision for the provided content."
                },
                "reason": {
                  "type": "string",
                  "description": "A concise, human-readable explanation for the moderation decision."
                }
              },
              "required": ["status", "reason"],
              "additionalProperties": false
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Calls the Azure OpenAI chat completions endpoint with the
     * {@code submit_moderation} tool and parses the result.
     *
     * @param client         An already-built {@link OpenAIClient}.
     * @param deploymentName The model deployment name (e.g., {@code gpt-4.1-mini-2025-04-14}).
     * @param content        The feedback text to evaluate.
     * @return Parsed {@link ModerationResult}.
     * @throws IllegalStateException    if the model returns no tool call or the
     *                                  JSON cannot be parsed.
     * @throws IllegalArgumentException if the {@code status} field value is unknown.
     */
    ModerationResult callModeration(OpenAIClient client,
                                    String deploymentName,
                                    String content) {
        log.debug("Building submit_moderation tool call for deployment '{}'", deploymentName);

        // ── Build function / tool definition ──────────────────────────────────
        ChatCompletionsFunctionToolDefinitionFunction functionSpec =
                new ChatCompletionsFunctionToolDefinitionFunction(TOOL_NAME)
                        .setDescription("Submit the moderation decision for the provided content.")
                        .setParameters(BinaryData.fromString(TOOL_SCHEMA));

        ChatCompletionsToolDefinition toolDefinition =
                new ChatCompletionsFunctionToolDefinition(functionSpec);

        // Force the model to call exactly this function
        ChatCompletionsToolSelection toolChoice =
                new ChatCompletionsToolSelection(
                        new ChatCompletionsNamedFunctionToolSelection(
                                new ChatCompletionsFunctionToolSelection(TOOL_NAME)));

        // ── Compose messages ───────────────────────────────────────────────────
        List<ChatRequestMessage> messages = new ArrayList<>();
        messages.add(new ChatRequestSystemMessage(SYSTEM_PROMPT));
        messages.add(new ChatRequestUserMessage(content));

        // ── Call the API ──────────────────────────────────────────────────────
        ChatCompletionsOptions options = new ChatCompletionsOptions(messages)
                .setTools(List.of(toolDefinition))
                .setToolChoice(toolChoice)
                .setTemperature(0.0);

        ChatCompletions response = client.getChatCompletions(deploymentName, options);

        // ── Extract the tool call arguments ────────────────────────────────────
        List<ChatCompletionsToolCall> toolCalls = response.getChoices()
                .get(0)
                .getMessage()
                .getToolCalls();

        if (toolCalls == null || toolCalls.isEmpty()) {
            throw new IllegalStateException(
                    "AI moderation did not produce a tool call – response was empty.");
        }

        ChatCompletionsToolCall rawCall = toolCalls.get(0);
        if (!(rawCall instanceof ChatCompletionsFunctionToolCall functionCall)) {
            throw new IllegalStateException(
                    "Unexpected tool call type: " + rawCall.getClass().getSimpleName());
        }

        String argumentsJson = functionCall.getFunction().getArguments();
        log.debug("Raw moderation arguments JSON: {}", argumentsJson);

        return parseArguments(argumentsJson);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ModerationResult parseArguments(String argumentsJson) {
        try {
            JsonNode root = objectMapper.readTree(argumentsJson);
            String statusStr = root.path("status").asText(null);
            String reason    = root.path("reason").asText("No reason provided.");

            if (statusStr == null || statusStr.isBlank()) {
                throw new IllegalStateException(
                        "AI moderation response is missing the 'status' field: " + argumentsJson);
            }

            ModerationStatus status = ModerationStatus.valueOf(statusStr.trim().toUpperCase());
            return new ModerationResult(status, reason);

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "AI moderation returned an unrecognised status value: " + argumentsJson, e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse AI moderation response: " + argumentsJson, e);
        }
    }
}

