package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.SignInRequestDTO;
import com.epam.edp.demo.dto.SignInResponseDTO;
import com.epam.edp.demo.dto.SignUpRequestDTO;
import com.epam.edp.demo.dto.SignUpResponseDTO;
import com.epam.edp.demo.dto.UserResponseDTO;
import com.epam.edp.demo.enums.Role;
import com.epam.edp.demo.exception.UnauthenticatedException;
import com.epam.edp.demo.model.User;
import com.epam.edp.demo.service.UserService;
import com.epam.edp.demo.service.PasswordResetService;
import com.epam.edp.demo.service.CaptchaService;
import com.epam.edp.demo.service.EmailVerificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private EmailVerificationService emailVerificationService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(userService, passwordResetService, captchaService, emailVerificationService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void signUp_returnsCreatedAndSuccessMessage() {
        SignUpRequestDTO req = signUpRequest();

        ResponseEntity<SignUpResponseDTO> response = authController.signUp(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Account created successfully", response.getBody().message());
    }

    @Test
    void signUp_delegatesToUserService() {
        SignUpRequestDTO req = signUpRequest();

        authController.signUp(req);

        verify(userService).signUp(req);
    }

    @Test
    void signUp_propagatesServiceExceptions() {
        SignUpRequestDTO req = signUpRequest();
        RuntimeException boom = new RuntimeException("boom");
        when(userService.signUp(req)).thenThrow(boom);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authController.signUp(req));

        assertEquals("boom", ex.getMessage());
    }

    @Test
    void signIn_returnsOkWithSignInResponse() {
        SignInRequestDTO req = signInRequest();
        SignInResponseDTO dto = new SignInResponseDTO("jwt", Role.CUSTOMER, "John Doe", "john@example.com", Instant.now());
        when(userService.signIn(req)).thenReturn(dto);

        ResponseEntity<SignInResponseDTO> response = authController.signIn(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void signIn_delegatesToUserService() {
        SignInRequestDTO req = signInRequest();
        when(userService.signIn(req)).thenReturn(new SignInResponseDTO("jwt", Role.CUSTOMER, "John Doe", "john@example.com", Instant.now()));

        authController.signIn(req);

        verify(userService).signIn(req);
    }

    @Test
    void signIn_propagatesServiceExceptions() {
        SignInRequestDTO req = signInRequest();
        RuntimeException boom = new RuntimeException("bad credentials");
        when(userService.signIn(req)).thenThrow(boom);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authController.signIn(req));

        assertEquals("bad credentials", ex.getMessage());
    }

    @Test
    void me_returnsCurrentUserResponseWhenAuthenticated() {
        authenticate("u-1", true);
        User user = user("u-1", "John", "Doe", "john@example.com", Role.CUSTOMER);
        when(userService.requireById("u-1")).thenReturn(user);

        ResponseEntity<UserResponseDTO> response = authController.me();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("u-1", response.getBody().id());
        assertEquals("john@example.com", response.getBody().email());
    }

    @Test
    void me_delegatesToRequireByIdWithAuthName() {
        authenticate("u-2", true);
        when(userService.requireById("u-2")).thenReturn(user("u-2", "A", "B", "ab@example.com", Role.CUSTOMER));

        authController.me();

        verify(userService).requireById("u-2");
    }

    @Test
    void me_throwsUnauthenticatedWhenAuthMissing() {
        SecurityContextHolder.clearContext();

        assertThrows(UnauthenticatedException.class, () -> authController.me());
    }

    @Test
    void me_throwsUnauthenticatedWithExpectedMessageWhenAuthMissing() {
        SecurityContextHolder.clearContext();

        UnauthenticatedException ex = assertThrows(UnauthenticatedException.class, () -> authController.me());

        assertEquals("Missing or invalid token", ex.getMessage());
    }

    @Test
    void me_throwsUnauthenticatedWhenNotAuthenticated() {
        authenticate("u-1", false);

        assertThrows(UnauthenticatedException.class, () -> authController.me());
    }

    @Test
    void me_throwsUnauthenticatedWhenPrincipalNull() {
        setAuthentication(new TestingAuthenticationToken(null, null, "ROLE_USER"));

        assertThrows(UnauthenticatedException.class, () -> authController.me());
    }

    @Test
    void me_propagatesServiceException() {
        authenticate("u-1", true);
        RuntimeException boom = new RuntimeException("not found");
        when(userService.requireById("u-1")).thenThrow(boom);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authController.me());

        assertEquals("not found", ex.getMessage());
    }

    @Test
    void signIn_returnsRoleFromService() {
        SignInRequestDTO req = signInRequest();
        SignInResponseDTO dto = new SignInResponseDTO("jwt", Role.ADMIN, "Admin User", "admin@example.com", Instant.now());
        when(userService.signIn(req)).thenReturn(dto);

        ResponseEntity<SignInResponseDTO> response = authController.signIn(req);

        assertEquals(Role.ADMIN, response.getBody().role());
    }

    @Test
    void signIn_returnsTokenFromService() {
        SignInRequestDTO req = signInRequest();
        SignInResponseDTO dto = new SignInResponseDTO("token-value", Role.CUSTOMER, "John Doe", "john@example.com", Instant.now());
        when(userService.signIn(req)).thenReturn(dto);

        ResponseEntity<SignInResponseDTO> response = authController.signIn(req);

        assertEquals("token-value", response.getBody().idToken());
    }

    @Test
    void signUp_responseIsStableAcrossCalls() {
        SignUpRequestDTO req = signUpRequest();

        ResponseEntity<SignUpResponseDTO> r1 = authController.signUp(req);
        ResponseEntity<SignUpResponseDTO> r2 = authController.signUp(req);

        assertEquals(r1.getBody().message(), r2.getBody().message());
    }

    @Test
    void me_returnsAdminRoleIfServiceReturnsAdmin() {
        authenticate("admin-1", true);
        when(userService.requireById("admin-1"))
                .thenReturn(user("admin-1", "Admin", "Root", "root@example.com", Role.ADMIN));

        ResponseEntity<UserResponseDTO> response = authController.me();

        assertEquals(Role.ADMIN, response.getBody().role());
    }

    @Test
    void me_keepsEmailFromUserEntity() {
        authenticate("u-1", true);
        when(userService.requireById("u-1"))
                .thenReturn(user("u-1", "Jane", "Doe", "jane@example.com", Role.CUSTOMER));

        ResponseEntity<UserResponseDTO> response = authController.me();

        assertEquals("jane@example.com", response.getBody().email());
    }

    @Test
    void me_keepsNameFieldsFromUserEntity() {
        authenticate("u-1", true);
        when(userService.requireById("u-1"))
                .thenReturn(user("u-1", "Jane", "Roe", "jane@example.com", Role.CUSTOMER));

        ResponseEntity<UserResponseDTO> response = authController.me();

        assertEquals("Jane", response.getBody().firstName());
        assertEquals("Roe", response.getBody().lastName());
    }

    @Test
    void me_allowsAnonymousPrincipalString_currentControllerBehavior() {
        authenticate("anonymousUser", true);
        when(userService.requireById("anonymousUser"))
                .thenReturn(user("anonymousUser", "Anon", "User", "anon@example.com", Role.CUSTOMER));

        ResponseEntity<UserResponseDTO> response = authController.me();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService).requireById("anonymousUser");
    }

    @Test
    void signUp_allowsDifferentPayloadsAndStillCreated() {
        SignUpRequestDTO req = new SignUpRequestDTO();
        req.setFirstName("Alice");
        req.setLastName("Walker");
        req.setEmail("alice@example.com");
        req.setPassword("Strong#123");

        ResponseEntity<SignUpResponseDTO> response = authController.signUp(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void signIn_allowsDifferentPayloadsAndStillDelegates() {
        SignInRequestDTO req = new SignInRequestDTO();
        req.setEmail("alice@example.com");
        req.setPassword("Strong#123");
        when(userService.signIn(req)).thenReturn(new SignInResponseDTO("jwt", Role.CUSTOMER, "Alice Walker", "alice@example.com", Instant.now()));

        ResponseEntity<SignInResponseDTO> response = authController.signIn(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService).signIn(req);
    }

    private static SignUpRequestDTO signUpRequest() {
        SignUpRequestDTO req = new SignUpRequestDTO();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail("john@example.com");
        req.setPassword("Strong#123");
        return req;
    }

    private static SignInRequestDTO signInRequest() {
        SignInRequestDTO req = new SignInRequestDTO();
        req.setEmail("john@example.com");
        req.setPassword("Strong#123");
        return req;
    }

    private static User user(String id, String firstName, String lastName, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setRole(role);
        user.setCreatedAt(Instant.now());
        return user;
    }

    private static void authenticate(String userId, boolean authenticated) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(userId, null, "ROLE_USER");
        token.setAuthenticated(authenticated);
        setAuthentication(token);
    }

    private static void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
