package com.epam.edp.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@Slf4j
public class CaptchaService {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final String secretKey;
    private final boolean enabled;
    private final RestTemplate restTemplate;

    public CaptchaService(
            @Value("${app.captcha.secret-key:}") String secretKey,
            @Value("${app.captcha.enabled:true}") boolean enabled) {
        this.secretKey = secretKey;
        this.enabled = enabled;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Verifies the CAPTCHA token with Google's reCAPTCHA API.
     * If CAPTCHA is disabled (e.g., local dev), this is a no-op.
     */
    public void verify(String captchaToken) {
        if (!enabled) {
            log.debug("CAPTCHA verification is disabled — skipping.");
            return;
        }

        if (captchaToken == null || captchaToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "CAPTCHA verification is required. Please complete the CAPTCHA challenge.");
        }

        String url = VERIFY_URL + "?secret=" + secretKey + "&response=" + captchaToken;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                log.warn("CAPTCHA verification failed: {}", response);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "CAPTCHA verification failed. Please try again.");
            }

            log.debug("CAPTCHA verified successfully.");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error contacting reCAPTCHA service: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "CAPTCHA verification service is unavailable. Please try again later.");
        }
    }
}
