package com.epam.edp.demo.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * US_16 - Social Media Logins
 *
 * Handles failed OAuth2 authentication.
 * Redirects the user to the frontend login page with a safe, user-friendly error message.
 * Internal exception details are NOT exposed to the frontend.
 *
 * Redirect example:
 *   http://localhost:3000/login?error=Social+login+failed.+Please+try+again.
 */
@Component
public class CustomOAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2FailureHandler.class);

    private final String failureUrl;

    public CustomOAuth2FailureHandler(
            @Value("${app.oauth2.redirect-failure-url}") String failureUrl) {
        this.failureUrl = failureUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        // Log the real cause internally for debugging
        log.warn("OAuth2 authentication failed: {}", exception.getMessage());

        // Distinguish between user-cancelled ("access_denied") and real failures.
        // This avoids showing "Social login failed" when the user simply clicked "Cancel" on the provider screen.
        String msg = exception.getMessage() != null ? exception.getMessage() : "";
        String userFacingMessage = msg.contains("access_denied")
                ? "Sign-in was cancelled."
                : "Social login failed. Please try again.";
        String safeMessage = URLEncoder.encode(userFacingMessage, StandardCharsets.UTF_8);
        String redirectUrl = failureUrl + safeMessage;

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
