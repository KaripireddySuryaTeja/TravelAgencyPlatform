package com.epam.edp.demo.config;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Azure OpenAI–powered content moderation.
 *
 * <h3>Security requirements</h3>
 * <ul>
 *   <li>The API key is read exclusively from the {@code AZURE_OPENAI_API_KEY}
 *       environment variable.</li>
 *   <li>In production (Kubernetes / KubeRocketCI) the variable must be injected
 *       via a Kubernetes Secret – see {@code deploy-templates/kubernetes/azure-openai-secret.yaml}.</li>
 *   <li>The placeholder value {@code "REPLACE_WITH_SECRET"} must NEVER appear in a
 *       running production container.</li>
 * </ul>
 *
 * <h3>Environment variables</h3>
 * <pre>
 * AZURE_OPENAI_API_KEY   – Azure OpenAI API key       (required in prod)
 * AZURE_OPENAI_ENDPOINT  – API endpoint URL            (default: https://your-resource.openai.azure.com/)
 * AZURE_OPENAI_DEPLOYMENT – Model deployment name      (default: gpt-4.1-mini-2025-04-14)
 * </pre>
 */
@Configuration
@Getter
public class AzureOpenAiConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AzureOpenAiConfiguration.class);

    /**
     * Azure OpenAI endpoint URL.
     * Override with {@code AZURE_OPENAI_ENDPOINT} env var.
     */
    @Value("${app.moderation.ai.endpoint:https://your-resource.openai.azure.com/}")
    private String endpoint;

    /**
     * Azure OpenAI API key.
     * Must be supplied via the {@code AZURE_OPENAI_API_KEY} environment variable.
     * The default {@code "REPLACE_WITH_SECRET"} will cause authentication failures –
     * it is intentionally unusable so misconfiguration fails fast.
     */
    @Value("${app.moderation.ai.api-key:REPLACE_WITH_SECRET}")
    private String apiKey;

    /**
     * Azure OpenAI model deployment name.
     * Override with {@code AZURE_OPENAI_DEPLOYMENT} env var.
     */
    @Value("${app.moderation.ai.deployment:gpt-4.1-mini-2025-04-14}")
    private String deployment;

    /**
     * Constructs and exposes a configured {@link OpenAIClient} bean.
     *
     * <p>The client is thread-safe and should be shared across the application –
     * Spring's singleton scope ensures a single instance is reused.</p>
     *
     * @return A ready-to-use {@link OpenAIClient}.
     */
    @Bean
    public OpenAIClient openAIClient() {
        log.info("[AzureOpenAI] Building OpenAI client — endpoint='{}', deployment='{}', keyPrefix='{}'",
                endpoint, deployment,
                (apiKey != null && apiKey.length() >= 4) ? apiKey.substring(0, 4) + "..." : "NOT_SET");
        if ("REPLACE_WITH_SECRET".equals(apiKey) || apiKey == null || apiKey.isBlank()) {
            log.error("[AzureOpenAI] *** AZURE_OPENAI_API_KEY is not set! AI features will fail. " +
                    "Please set AZURE_OPENAI_API_KEY in your .env file. ***");
        }
        return new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey.trim()))
                .buildClient();
    }

    @PostConstruct
    public void logConfig() {
        log.info("[AzureOpenAI] Configuration loaded — endpoint='{}', deployment='{}', keyLength={}",
                endpoint, deployment, (apiKey != null ? apiKey.length() : 0));
    }
}

