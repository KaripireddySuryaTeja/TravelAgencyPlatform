package com.epam.edp.demo.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epam.edp.demo.dto.SignInRequestDTO;
import com.epam.edp.demo.dto.SignInResponseDTO;
import com.epam.edp.demo.dto.SignUpRequestDTO;
import com.epam.edp.demo.dto.SignUpResponseDTO;
import com.epam.edp.demo.dto.UserResponseDTO;
import com.epam.edp.demo.dto.ApiErrorResponseDTO;
import com.epam.edp.demo.dto.AuthUserResponseDTO;
import com.epam.edp.demo.dto.LoginUrlResponseDTO;
import com.epam.edp.demo.dto.PasswordResetRequestDTO;
import com.epam.edp.demo.dto.VerifyResetCodeDTO;
import com.epam.edp.demo.dto.ResetPasswordDTO;
import com.epam.edp.demo.dto.EmailVerificationRequestDTO;
import com.epam.edp.demo.dto.VerifyEmailCodeDTO;
import com.epam.edp.demo.exception.UnauthenticatedException;
import com.epam.edp.demo.model.User;
import com.epam.edp.demo.service.UserService;
import com.epam.edp.demo.service.PasswordResetService;
import com.epam.edp.demo.service.CaptchaService;
import com.epam.edp.demo.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and current user endpoints")
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final CaptchaService captchaService;
    private final EmailVerificationService emailVerificationService;

    @Value("${app.server.url}")
    private String serverUrl;

    public AuthController(UserService userService, PasswordResetService passwordResetService, CaptchaService captchaService, EmailVerificationService emailVerificationService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
        this.captchaService = captchaService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/sign-up")
        @Operation(summary = "Register a new user", description = "Creates a user account with email/password credentials.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                content = @Content(schema = @Schema(implementation = SignUpResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or weak password",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Email already exists",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class)))
        })
            public ResponseEntity<SignUpResponseDTO> signUp(
                @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Registration details for the new user account.",
                    required = true)
                @Valid @RequestBody SignUpRequestDTO request) {
        captchaService.verify(request.getCaptchaToken());
        userService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SignUpResponseDTO.ok());
    }

    @GetMapping("/check-email")
    @Operation(summary = "Check email availability", description = "Returns whether an email is already registered.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email availability status")
    })
    public ResponseEntity<Map<String, Boolean>> checkEmail(@org.springframework.web.bind.annotation.RequestParam String email) {
        boolean exists = userService.emailExists(email.trim().toLowerCase(java.util.Locale.ROOT));
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PostMapping("/sign-in")
        @Operation(summary = "Sign in", description = "Authenticates a user and returns an access token.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Signed in successfully",
                content = @Content(schema = @Schema(implementation = SignInResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class))),
            @ApiResponse(responseCode = "423", description = "Account temporarily locked",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class)))
        })
            public ResponseEntity<SignInResponseDTO> signIn(
                @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Email/password credentials used to obtain a JWT token.",
                    required = true)
                @Valid @RequestBody SignInRequestDTO request) {
        return ResponseEntity.ok(userService.signIn(request));
    }

    @GetMapping("/me")
        @Operation(summary = "Get current user", description = "Returns profile details of the authenticated user.")
        @SecurityRequirement(name = "bearerAuth")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current user details",
                content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class)))
        })
    public ResponseEntity<UserResponseDTO> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new UnauthenticatedException("Missing or invalid token");
        }
        String userId = auth.getName();
        User user = userService.requireById(userId);
        return ResponseEntity.ok(UserResponseDTO.from(user));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDTO request) {
        passwordResetService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "If an account exists with this email, you will receive a verification code shortly"));
    }

    @PostMapping("/password-reset/verify")
    public ResponseEntity<Map<String, String>> verifyResetCode(@Valid @RequestBody VerifyResetCodeDTO request) {
        passwordResetService.verifyResetCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(Map.of("message", "Verification code is valid"));
    }

    @PostMapping("/password-reset/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordDTO request) {
        passwordResetService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }

    @PostMapping("/email-verification/request")
    @Operation(summary = "Request email verification code", description = "Sends a verification code to the provided email for registration.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verification code sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid email format")
    })
    public ResponseEntity<Map<String, String>> requestEmailVerification(@Valid @RequestBody EmailVerificationRequestDTO request) {
        emailVerificationService.requestEmailVerification(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Verification code has been sent to your email"));
    }

    @PostMapping("/email-verification/verify")
    @Operation(summary = "Verify email code", description = "Verifies the email verification code sent during registration.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email verification code is valid"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired verification code")
    })
    public ResponseEntity<Map<String, String>> verifyEmailCode(@Valid @RequestBody VerifyEmailCodeDTO request) {
        emailVerificationService.verifyEmailCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(Map.of("message", "Email verification code is valid"));
    }

    /**
     * US_16 - Social Media Logins
     *
     * Returns the OAuth2 login initiation URL for Google.
     * Frontend should redirect the user to this URL to start the Google login flow.
     *
     * GET /api/v1/auth/login-url/google
     * Response: { "provider": "google", "loginUrl": "http://localhost:8080/oauth2/authorization/google" }
     */
    @GetMapping("/login-url/google")
    @Operation(summary = "Get Google OAuth2 login URL",
               description = "Returns the URL to initiate Google social login. Frontend redirects user to this URL.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Google login URL",
            content = @Content(schema = @Schema(implementation = LoginUrlResponseDTO.class)))
    })
    public ResponseEntity<LoginUrlResponseDTO> googleLoginUrl() {
        return ResponseEntity.ok(new LoginUrlResponseDTO("google", serverUrl + "/oauth2/authorization/google"));
    }

    /**
     * US_16 - Social Media Logins
     *
     * Returns the OAuth2 login initiation URL for GitHub.
     * Frontend should redirect the user to this URL to start the GitHub login flow.
     *
     * GET /api/v1/auth/login-url/github
     * Response: { "provider": "github", "loginUrl": "http://localhost:8080/oauth2/authorization/github" }
     */
    @GetMapping("/login-url/github")
    @Operation(summary = "Get GitHub OAuth2 login URL",
               description = "Returns the URL to initiate GitHub social login. Frontend redirects user to this URL.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "GitHub login URL",
            content = @Content(schema = @Schema(implementation = LoginUrlResponseDTO.class)))
    })
    public ResponseEntity<LoginUrlResponseDTO> githubLoginUrl() {
        return ResponseEntity.ok(new LoginUrlResponseDTO("github", serverUrl + "/oauth2/authorization/github"));
    }

    /**
     * US_16 - Social Media Logins
     *
     * Returns the OAuth2 login initiation URL for Facebook.
     * Frontend should redirect the user to this URL to start the Facebook login flow.
     *
     * GET /api/v1/auth/login-url/facebook
     * Response: { "provider": "facebook", "loginUrl": "http://localhost:8080/oauth2/authorization/facebook" }
     */
    @GetMapping("/login-url/facebook")
    @Operation(summary = "Get Facebook OAuth2 login URL",
               description = "Returns the URL to initiate Facebook social login. Frontend redirects user to this URL.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Facebook login URL",
            content = @Content(schema = @Schema(implementation = LoginUrlResponseDTO.class)))
    })
    public ResponseEntity<LoginUrlResponseDTO> facebookLoginUrl() {
        return ResponseEntity.ok(new LoginUrlResponseDTO("facebook", serverUrl + "/oauth2/authorization/facebook"));
    }

    /**
     * US_16 - Social Media Logins
     *
     * Returns current authenticated user details including OAuth2 provider info.
     * Works for both standard JWT login and social login users.
     *
     * GET /api/v1/auth/social/me
     */
    @GetMapping("/social/me")
    @Operation(summary = "Get current user with provider info",
               description = "Returns profile including OAuth2 provider for social login users.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Current user details with provider",
            content = @Content(schema = @Schema(implementation = AuthUserResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(schema = @Schema(implementation = ApiErrorResponseDTO.class)))
    })
    public ResponseEntity<AuthUserResponseDTO> socialMe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new UnauthenticatedException("Missing or invalid token");
        }
        String userId = auth.getName();
        User user = userService.requireById(userId);
        return ResponseEntity.ok(AuthUserResponseDTO.from(user));
    }
}

