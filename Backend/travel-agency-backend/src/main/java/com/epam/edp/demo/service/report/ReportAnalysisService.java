package com.epam.edp.demo.service.report;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.epam.edp.demo.config.AzureOpenAiConfiguration;
import com.epam.edp.demo.model.report.AnalysisResult;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Calls the Azure OpenAI (EPAM DIAL) endpoint to analyse report content
 * and parse the structured JSON response into an {@link AnalysisResult}.
 *
 * <p>Uses the {@link OpenAIClient} bean from {@link AzureOpenAiConfiguration}
 * so credentials are never duplicated.</p>
 */
@Service
public class ReportAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ReportAnalysisService.class);

    // ── System prompt ──────────────────────────────────────────────────────────
    private static final String SYSTEM_PROMPT = """
            You are an expert business analyst AI. Analyze the provided report data and \
            return a structured JSON response with the following fields:
            
            {
              "summary": "2-3 sentence executive summary",
              "keyTrends": ["trend1", "trend2"],
              "anomalies": ["anomaly1", "anomaly2"],
              "potentialIssues": ["issue1", "issue2"],
              "actionableRecommendations": ["rec1", "rec2"],
              "riskLevel": "LOW | MEDIUM | HIGH",
              "dataQualityScore": 0-100,
              "charts": [
                {
                  "title": "chart title",
                  "type": "bar | line | pie",
                  "labels": ["label1", "label2"],
                  "values": [val1, val2]
                }
              ]
            }
            
            Respond ONLY with valid JSON. No markdown, no explanation outside the JSON.
            """;

    private final OpenAIClient openAIClient;
    private final AzureOpenAiConfiguration aiConfig;
    private final ObjectMapper objectMapper;

    public ReportAnalysisService(OpenAIClient openAIClient,
                                  AzureOpenAiConfiguration aiConfig) {
        this.openAIClient = openAIClient;
        this.aiConfig = aiConfig;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Sends the extracted report content to the AI and returns structured insights.
     *
     * @param reportContent parsed text content of the uploaded report file
     * @param reportType    "STAFF" or "BUSINESS_ACTIVITY"
     * @return parsed {@link AnalysisResult}
     * @throws RuntimeException if the AI call fails or returns unparseable JSON
     */
    public AnalysisResult analyzeReport(String reportContent, String reportType) {
        String deploymentName = aiConfig.getDeployment();
        logger.info("Starting AI report analysis for type='{}', deployment='{}'",
                reportType, deploymentName);

        List<ChatRequestMessage> prompts = new ArrayList<>();
        prompts.add(new ChatRequestSystemMessage(SYSTEM_PROMPT));
        prompts.add(new ChatRequestUserMessage(
                "Report Type: " + reportType + "\n\n" + reportContent));

        ChatCompletionsOptions options = new ChatCompletionsOptions(prompts)
                .setMaxTokens(6553)
                .setTemperature(0.7)
                .setTopP(0.95)
                .setFrequencyPenalty((double) 0)
                .setPresencePenalty((double) 0)
                .setStop(null);

        try {
            ChatCompletions chatCompletions =
                    openAIClient.getChatCompletions(deploymentName, options);

            String resultJson = chatCompletions.getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

            logger.info("AI raw response received ({} chars)", resultJson != null ? resultJson.length() : 0);
            logger.debug("AI raw response: {}", resultJson);

            // Strip potential markdown code fences (defensive)
            String cleaned = stripMarkdownFences(resultJson);

            AnalysisResult result = objectMapper.readValue(cleaned, AnalysisResult.class);
            logger.info("AI analysis complete — riskLevel={}, dataQualityScore={}",
                    result.getRiskLevel(), result.getDataQualityScore());
            return result;

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error("Failed to parse AI JSON response: {}", e.getMessage());
            throw new RuntimeException("AI_FORMAT_ERROR: " + e.getMessage(), e);
        } catch (com.azure.core.exception.HttpResponseException e) {
            int statusCode = e.getResponse() != null ? e.getResponse().getStatusCode() : -1;
            logger.error("AI HTTP error — status={}, message={}", statusCode, e.getMessage());
            if (statusCode == 401 || statusCode == 403) {
                logger.error("[AzureOpenAI] Authentication failed ({}). Check AZURE_OPENAI_API_KEY in .env", statusCode);
                throw new RuntimeException("SERVICE_UNAVAILABLE: AI authentication error (HTTP " + statusCode +
                        "). Check AZURE_OPENAI_API_KEY in .env", e);
            }
            throw new RuntimeException("SERVICE_UNAVAILABLE: AI HTTP error " + statusCode + ": " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("AI analysis failed — type={}, message={}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("AI analysis root cause — type={}, message={}",
                        e.getCause().getClass().getName(), e.getCause().getMessage());
            }
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("timeout") || msg.contains("connect") || msg.contains("timed out")) {
                throw new RuntimeException("SERVICE_UNAVAILABLE: " + e.getMessage(), e);
            }
            throw new RuntimeException("SERVICE_UNAVAILABLE: AI service error: " + e.getMessage(), e);
        }
    }

    /** Remove optional ```json ... ``` wrappers that some model versions emit. */
    private String stripMarkdownFences(String json) {
        if (json == null) return "{}";
        String trimmed = json.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("(?s)^```[a-zA-Z]*\\s*", "")
                             .replaceAll("(?s)\\s*```$", "")
                             .trim();
        }
        return trimmed;
    }
}

