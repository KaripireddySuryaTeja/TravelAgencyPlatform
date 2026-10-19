package com.epam.edp.demo.security;

import com.epam.edp.demo.enums.AuthProvider;
import com.epam.edp.demo.model.User;
import com.epam.edp.demo.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * US_16 - Social Media Logins
 *
 * Handles successful OAuth2 authentication:
 * 1. Retrieves user from database by email from OAuth2 principal
 * 2. Generates our application JWT token using the existing JwtService
 * 3. Redirects frontend to the success URL with token as query parameter
 *    e.g., http://localhost:3000/oauth-success?token=<JWT>&role=CUSTOMER
 *
 * In production, prefer HttpOnly cookie over query parameter for token delivery.
 */
@Component
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2SuccessHandler.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final String successUrl;
    private final String failureUrl;

    public CustomOAuth2SuccessHandler(
            JwtService jwtService,
            UserRepository userRepository,
            @Value("${app.oauth2.redirect-success-url}") String successUrl,
            @Value("${app.oauth2.redirect-failure-url}") String failureUrl) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.successUrl = successUrl;
        this.failureUrl = failureUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String providerName = ((org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) authentication)
                .getAuthorizedClientRegistrationId();

        // Extract email from the OAuth2 principal attributes.
        // For GitHub users with private emails, the email may be null here — the real email was
        // fetched from the GitHub Emails API in CustomOAuth2UserService and saved to DB.
        String email = oAuth2User.getAttribute("email");

        User user = null;

        if (email != null && !email.isBlank()) {
            user = userRepository.findByEmail(email.toLowerCase()).orElse(null);
        }

        // Fallback for GitHub private email: look up by provider + providerId
        if (user == null) {
            AuthProvider authProvider = resolveProvider(providerName);
            Object rawId = oAuth2User.getAttribute("id");   // GitHub: Integer
            String subId = oAuth2User.getAttribute("sub");   // Google: String
            String providerId = rawId != null ? String.valueOf(rawId) : subId;
            if (providerId != null) {
                user = userRepository.findByProviderAndProviderId(authProvider, providerId).orElse(null);
                if (user != null) {
                    log.info("OAuth2 success handler: found user via providerId fallback for provider={}", providerName);
                }
            }
        }

        if (user == null) {
            log.error("OAuth2 success handler: user not found in database for provider={}", providerName);
            String safeMessage = URLEncoder.encode("Social login failed. Please try again.", StandardCharsets.UTF_8);
            getRedirectStrategy().sendRedirect(request, response, failureUrl + safeMessage);
            return;
        }

        // Generate our application JWT token using the existing JwtService
        // Success handler generates JWT for the authenticated social login user
        JwtService.IssuedToken issuedToken = jwtService.issue(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getRole().name()
        );

        // Redirect frontend to the success URL with token as query parameter (development approach)
        // Frontend should extract token and store it (e.g., localStorage) then use it for API calls
        String redirectUrl = UriComponentsBuilder.fromUriString(successUrl)
                .queryParam("token", issuedToken.token())
                .queryParam("role", user.getRole().name())
                .build(true)
                .toUriString();

        log.info("OAuth2 social login successful for user: {}, redirecting to frontend", user.getEmail());
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private AuthProvider resolveProvider(String providerName) {
        if (providerName == null) return AuthProvider.LOCAL;
        return switch (providerName.toLowerCase()) {
            case "google"   -> AuthProvider.GOOGLE;
            case "github"   -> AuthProvider.GITHUB;
            case "facebook" -> AuthProvider.FACEBOOK;
            default         -> AuthProvider.LOCAL;
        };
    }
}
