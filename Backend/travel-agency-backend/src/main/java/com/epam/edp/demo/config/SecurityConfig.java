package com.epam.edp.demo.config;


import com.epam.edp.demo.security.CustomOAuth2FailureHandler;
import com.epam.edp.demo.security.CustomOAuth2SuccessHandler;
import com.epam.edp.demo.security.CustomOAuth2UserService;
import com.epam.edp.demo.security.GithubForceLoginResolver;
import com.epam.edp.demo.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Security configuration supporting both:
 * - Standard JWT-based authentication (existing login/register flow)
 * - OAuth2 social login via Google and Facebook (US_16)
 *
 * Session policy is IF_REQUIRED to allow the OAuth2 authorization code flow
 * (which requires a session for the state/CSRF parameter during the redirect).
 * All regular API calls remain stateless via the JWT filter.
 */
@Configuration
public class SecurityConfig {

    private static final int BCRYPT_STRENGTH = 12;

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
    private final CustomOAuth2FailureHandler customOAuth2FailureHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            CustomOAuth2UserService customOAuth2UserService,
            CustomOAuth2SuccessHandler customOAuth2SuccessHandler,
            CustomOAuth2FailureHandler customOAuth2FailureHandler) {
        this.jwtFilter = jwtFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOAuth2SuccessHandler = customOAuth2SuccessHandler;
        this.customOAuth2FailureHandler = customOAuth2FailureHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            // IF_REQUIRED allows OAuth2 code flow to use sessions for state parameter.
            // JWT filter still handles all API calls statelessly.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/**",
                    // OAuth2 login initiation: /oauth2/authorization/google, /oauth2/authorization/facebook
                    "/oauth2/**",
                    // OAuth2 provider callback: /login/oauth2/code/google, /login/oauth2/code/facebook
                    "/login/oauth2/**",
                    "/login/**",
                    "/error",
                    "/actuator/health",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs",
                    "/api-docs/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/webjars/**"
                ).permitAll()
                .requestMatchers("/api/v1/bookings/**").authenticated()
                .requestMatchers(
                    org.springframework.http.HttpMethod.GET,
                    "/api/v1/tours/*/feedback"
                ).permitAll()
                .requestMatchers("/api/v1/tours/*/feedback").authenticated()
                .requestMatchers("/api/v1/reports/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // Report Analysis AI endpoints — admin only
                .requestMatchers("/api/reports/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/users/**").authenticated()
                .anyRequest().permitAll()
            )
            // OAuth2 social login configuration (US_16)
            // Flow: /oauth2/authorization/google → Google → /login/oauth2/code/google → success/failure handler
            .oauth2Login(oauth -> oauth
                .authorizationEndpoint(endpoint -> endpoint
                    // Force GitHub to show the login/account-selection page on every attempt
                    // so users can switch GitHub accounts without having to manually log out of GitHub.
                    .authorizationRequestResolver(new GithubForceLoginResolver(clientRegistrationRepository))
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(customOAuth2SuccessHandler)
                .failureHandler(customOAuth2FailureHandler)
            )
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                res.setStatus(HttpStatus.UNAUTHORIZED.value());
                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("timestamp", Instant.now().toString());
                body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
                body.put("error", "Unauthenticated");
                body.put("message", "Authentication is required");
                res.getWriter().write(mapper.writeValueAsString(body));
            }))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            // Existing JWT filter authenticates all API requests statelessly
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}



