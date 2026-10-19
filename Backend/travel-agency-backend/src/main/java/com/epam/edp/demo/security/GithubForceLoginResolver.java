package com.epam.edp.demo.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * US_16 - Social Media Logins (GitHub account switching fix)
 *
 * Custom OAuth2 authorization request resolver for GitHub.
 *
 * Problem: GitHub remembers previously authorized OAuth apps and auto-approves
 * subsequent login attempts without showing the sign-in / account-selection screen.
 * This makes it impossible for users to switch GitHub accounts from within our app.
 *
 * Fix: Inject the 'login' parameter with an empty value into every GitHub
 * authorization request. GitHub interprets this as "show the login form so the
 * user can confirm or change the account", which breaks the silent auto-approval.
 *
 * All other providers (Google, Facebook) are passed through unchanged.
 */
public class GithubForceLoginResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public GithubForceLoginResolver(ClientRegistrationRepository repo) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return forceGithubLogin(delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return forceGithubLogin(delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest forceGithubLogin(OAuth2AuthorizationRequest request) {
        if (request == null) return null;
        // Only modify GitHub authorization requests
        if (!request.getAuthorizationUri().contains("github.com")) return request;
        // Add 'login' with an empty value — GitHub uses this to show the sign-in form
        // instead of silently re-authorizing the previously used account.
        Map<String, Object> params = new HashMap<>(request.getAdditionalParameters());
        params.put("login", "");
        return OAuth2AuthorizationRequest.from(request)
                .additionalParameters(params)
                .build();
    }
}
