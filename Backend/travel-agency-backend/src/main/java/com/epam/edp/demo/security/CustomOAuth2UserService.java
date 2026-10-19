package com.epam.edp.demo.security;

import com.epam.edp.demo.enums.AuthProvider;
import com.epam.edp.demo.enums.Role;
import com.epam.edp.demo.model.User;
import com.epam.edp.demo.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

/**
 * US_16 - Social Media Logins
 *
 * Custom OAuth2 user service that handles social login for Google, GitHub, and Facebook.
 *
 * Flow:
 * 1. OAuth2 login starts from /oauth2/authorization/google (or /github)
 * 2. Provider authenticates user and redirects back to /login/oauth2/code/google
 * 3. Spring Security exchanges the code for user info, then calls this service
 * 4. We extract email/name/providerId from provider attributes
 * 5. If email exists in DB → link social account to existing user
 * 6. If email not found → create new user with role CUSTOMER
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    private static final String GITHUB_EMAILS_API = "https://api.github.com/user/emails";

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Load user info from the OAuth2 provider (Google/GitHub/Facebook)
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Extract provider name (e.g., "google", "github", "facebook")
        String providerName = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider authProvider = resolveProvider(providerName);

        // Extract user attributes from the provider's user-info endpoint
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = extractEmail(attributes, authProvider, userRequest);
        String name = extractName(attributes, authProvider);
        String providerId = extractProviderId(attributes, authProvider);
        String picture = extractPicture(attributes, authProvider);

        // Email is required — must be present for account creation/linking
        if (email == null || email.isBlank()) {
            log.error("OAuth2 provider '{}' did not return an email address", providerName);
            String userMessage = authProvider == AuthProvider.GITHUB
                    ? "Email not available from GitHub. Please make your GitHub email public or use Google login."
                    : "Email not returned by provider. Please ensure your account has an email address.";
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_email"), userMessage);
        }

        // Find or create the user in our database
        processUser(email.toLowerCase(), name, providerId, picture, authProvider);

        // For GitHub with private email: the email was fetched from the GitHub Emails API
        // but is absent from the original oAuth2User attributes (GitHub doesn't include it).
        // Inject it into a new DefaultOAuth2User so CustomOAuth2SuccessHandler can reliably
        // look up the user by email instead of relying on the brittle providerId fallback.
        if (attributes.get("email") == null) {
            Map<String, Object> enriched = new HashMap<>(attributes);
            enriched.put("email", email.toLowerCase());
            String nameAttrKey = userRequest.getClientRegistration()
                    .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
            return new DefaultOAuth2User(oAuth2User.getAuthorities(), enriched, nameAttrKey);
        }

        return oAuth2User;
    }

    /**
     * Existing email → link social provider to existing user (account linking).
     * No existing email → create a new user account with role CUSTOMER.
     */
    private void processUser(String email, String name, String providerId,
                             String picture, AuthProvider authProvider) {
        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        if (existingUserOpt.isPresent()) {
            User user = existingUserOpt.get();
            boolean updated = false;

            if (user.getProvider() == AuthProvider.LOCAL && providerId != null) {
                user.setProvider(authProvider);
                user.setProviderId(providerId);
                updated = true;
                log.info("Linked {} account to existing user: {}", authProvider, email);
            }

            if (picture != null && user.getImageUrl() == null) {
                user.setImageUrl(picture);
                updated = true;
            }

            if (updated) {
                userRepository.save(user);
            }
        } else {
            String firstName;
            String lastName = "";
            if (name != null && !name.isBlank()) {
                int spaceIdx = name.indexOf(' ');
                if (spaceIdx > 0) {
                    firstName = name.substring(0, spaceIdx).trim();
                    lastName = name.substring(spaceIdx + 1).trim();
                } else {
                    firstName = name.trim();
                }
            } else {
                firstName = email.split("@")[0];
            }

            User newUser = new User(firstName, lastName, email, null, Role.CUSTOMER);
            newUser.setProvider(authProvider);
            newUser.setProviderId(providerId);
            newUser.setImageUrl(picture);

            userRepository.save(newUser);
            log.info("Created new user via {} social login: {}", authProvider, email);
        }
    }

    private AuthProvider resolveProvider(String providerName) {
        return switch (providerName.toLowerCase()) {
            case "google"   -> AuthProvider.GOOGLE;
            case "github"   -> AuthProvider.GITHUB;
            case "facebook" -> AuthProvider.FACEBOOK;
            default -> throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"),
                    "Unsupported OAuth2 provider: " + providerName);
        };
    }

    private String extractEmail(Map<String, Object> attributes, AuthProvider provider,
                                OAuth2UserRequest userRequest) {
        return switch (provider) {
            case GOOGLE   -> (String) attributes.get("email");
            case FACEBOOK -> (String) attributes.get("email");
            case GITHUB   -> {
                // GitHub may return null email when the user's email is set to private.
                // In that case, we call the GitHub Emails API with the access token
                // to find the user's primary verified email address.
                String email = (String) attributes.get("email");
                if (email == null || email.isBlank()) {
                    log.info("GitHub email not in user-info response; fetching from GitHub Emails API");
                    email = fetchGithubPrimaryEmail(userRequest.getAccessToken().getTokenValue());
                }
                yield email;
            }
            default -> null;
        };
    }

    /**
     * Calls GET https://api.github.com/user/emails and returns the primary verified email.
     * Returns null if the call fails or no primary verified email is found.
     */
    private String fetchGithubPrimaryEmail(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/vnd.github+json");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    GITHUB_EMAILS_API,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getBody() != null) {
                return response.getBody().stream()
                        .filter(e -> Boolean.TRUE.equals(e.get("primary"))
                                  && Boolean.TRUE.equals(e.get("verified")))
                        .map(e -> (String) e.get("email"))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch GitHub primary email from API: {}", ex.getMessage());
        }
        return null;
    }

    private String extractName(Map<String, Object> attributes, AuthProvider provider) {
        // Google, GitHub, and Facebook all return a 'name' attribute.
        // For GitHub, 'name' may be null if the user hasn't set a display name;
        // fall back to 'login' (username) in that case.
        String name = (String) attributes.get("name");
        if ((name == null || name.isBlank()) && provider == AuthProvider.GITHUB) {
            name = (String) attributes.get("login");
        }
        return name;
    }

    private String extractProviderId(Map<String, Object> attributes, AuthProvider provider) {
        return switch (provider) {
            // Google uses 'sub' as the unique user ID
            case GOOGLE -> (String) attributes.get("sub");
            // GitHub uses 'id' (integer) as the unique user ID
            case GITHUB -> {
                Object id = attributes.get("id");
                yield id != null ? String.valueOf(id) : null;
            }
            // Facebook uses 'id' as the unique user ID
            case FACEBOOK -> {
                Object id = attributes.get("id");
                yield id != null ? String.valueOf(id) : null;
            }
            default -> null;
        };
    }

    private String extractPicture(Map<String, Object> attributes, AuthProvider provider) {
        return switch (provider) {
            // Google returns 'picture' as a direct URL string
            case GOOGLE -> (String) attributes.get("picture");
            // GitHub returns 'avatar_url' as a direct URL string
            case GITHUB -> (String) attributes.get("avatar_url");
            // Facebook returns 'picture' as a nested object:
            // { "data": { "url": "https://...", "width": 50, "height": 50, "is_silhouette": false } }
            case FACEBOOK -> {
                Object pic = attributes.get("picture");
                if (pic instanceof String s) yield s;
                if (pic instanceof Map<?, ?> picMap) {
                    Object data = picMap.get("data");
                    if (data instanceof Map<?, ?> dataMap) {
                        Object url = dataMap.get("url");
                        if (url instanceof String urlStr) yield urlStr;
                    }
                }
                yield null;
            }
            default -> null;
        };
    }
}
