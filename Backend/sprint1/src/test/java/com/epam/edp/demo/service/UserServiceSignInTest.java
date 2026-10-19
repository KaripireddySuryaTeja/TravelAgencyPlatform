package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.SignInRequestDTO;
import com.epam.edp.demo.dto.SignInResponseDTO;
import com.epam.edp.demo.enums.Role;
import com.epam.edp.demo.exception.AccountLockedException;
import com.epam.edp.demo.exception.InvalidCredentialsException;
import com.epam.edp.demo.model.User;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceSignInTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(repository, passwordEncoder, jwtService, 3, 10);
    }

    @Test
    void signIn_normalizesEmailBeforeLookup() {
        SignInRequestDTO req = request("  John.Doe@Example.COM  ", "password");
        User user = user("u-1", "john.doe@example.com");
        when(repository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", user.getPasswordHash())).thenReturn(true);
        when(repository.save(user)).thenReturn(user);
        when(jwtService.issue("u-1", "john.doe@example.com", "John", "CUSTOMER"))
                .thenReturn(new JwtService.IssuedToken("jwt", Instant.now().plusSeconds(3600)));

        userService.signIn(req);

        verify(repository).findByEmail("john.doe@example.com");
    }

    @Test
    void signIn_invalidCredentialsWhenUserNotFound() {
        when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> userService.signIn(request("missing@example.com", "pw")));

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void signIn_throwsAccountLockedWhenLockStillActive() {
        User user = user("u-1", "john@example.com");
        user.setLockedUntil(Instant.now().plusSeconds(120));
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        AccountLockedException ex = assertThrows(AccountLockedException.class,
                () -> userService.signIn(request("john@example.com", "pw")));

        assertNotNull(ex.getRetryAfterSeconds());
        verify(repository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void signIn_expiredLockIsClearedBeforePasswordCheck() {
        User user = user("u-1", "john@example.com");
        user.setFailedLoginAttempts(2);
        user.setLockedUntil(Instant.now().minusSeconds(10));
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw", user.getPasswordHash())).thenReturn(false);
        when(repository.save(user)).thenReturn(user);

        assertThrows(InvalidCredentialsException.class,
                () -> userService.signIn(request("john@example.com", "pw")));

        assertNull(user.getLockedUntil());
        assertEquals(1, user.getFailedLoginAttempts());
        verify(repository).save(user);
    }

    @Test
    void signIn_incrementsFailedAttemptsAndThrowsInvalidBeforeThreshold() {
        User user = user("u-1", "john@example.com");
        user.setFailedLoginAttempts(1);
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", user.getPasswordHash())).thenReturn(false);
        when(repository.save(user)).thenReturn(user);

        assertThrows(InvalidCredentialsException.class,
                () -> userService.signIn(request("john@example.com", "bad")));

        assertEquals(2, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
        verify(repository).save(user);
        verifyNoInteractions(jwtService);
    }

    @Test
    void signIn_locksAccountAtThresholdAndThrowsAccountLocked() {
        User user = user("u-1", "john@example.com");
        user.setFailedLoginAttempts(2);
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", user.getPasswordHash())).thenReturn(false);
        when(repository.save(user)).thenReturn(user);

        AccountLockedException ex = assertThrows(AccountLockedException.class,
                () -> userService.signIn(request("john@example.com", "bad")));

        assertEquals(600, ex.getRetryAfterSeconds());
        assertEquals(3, user.getFailedLoginAttempts());
        assertNotNull(user.getLockedUntil());
        verify(repository).save(user);
        verifyNoInteractions(jwtService);
    }

    @Test
    void signIn_successResetsCountersAndReturnsTokenResponse() {
        User user = user("u-1", "john@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole(Role.CUSTOMER);
        user.setFailedLoginAttempts(2);
        user.setLockedUntil(Instant.now().minusSeconds(10));

        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", user.getPasswordHash())).thenReturn(true);
        when(repository.save(user)).thenReturn(user);
        Instant exp = Instant.now().plusSeconds(3600);
        when(jwtService.issue("u-1", "john@example.com", "John", "CUSTOMER"))
                .thenReturn(new JwtService.IssuedToken("jwt-token", exp));

        SignInResponseDTO response = userService.signIn(request("john@example.com", "good"));

        assertEquals("jwt-token", response.idToken());
        assertEquals(Role.CUSTOMER, response.role());
        assertEquals("John Doe", response.userName());
        assertEquals("john@example.com", response.email());
        assertEquals(exp, response.expiresAt());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }

    @Test
    void signIn_usesSavedEntityDataForResponse() {
        User found = user("u-1", "john@example.com");
        found.setFirstName("John");
        found.setLastName("Doe");
        found.setRole(Role.CUSTOMER);

        User saved = user("u-1", "john@example.com");
        saved.setFirstName("Johnny");
        saved.setLastName("Doer");
        saved.setRole(Role.ADMIN);

        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(found));
        when(passwordEncoder.matches("good", found.getPasswordHash())).thenReturn(true);
        when(repository.save(found)).thenReturn(saved);
        when(jwtService.issue("u-1", "john@example.com", "Johnny", "ADMIN"))
                .thenReturn(new JwtService.IssuedToken("jwt", Instant.now().plusSeconds(3600)));

        SignInResponseDTO response = userService.signIn(request("john@example.com", "good"));

        assertEquals(Role.ADMIN, response.role());
        assertEquals("Johnny Doer", response.userName());
    }

    @Test
    void signIn_callsSaveBeforeIssuingJwt() {
        User user = user("u-1", "john@example.com");
        user.setFirstName("John");
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", user.getPasswordHash())).thenReturn(true);
        when(repository.save(user)).thenReturn(user);
        when(jwtService.issue("u-1", "john@example.com", "John", "CUSTOMER"))
                .thenReturn(new JwtService.IssuedToken("jwt", Instant.now().plusSeconds(3600)));

        userService.signIn(request("john@example.com", "good"));

        InOrder inOrder = inOrder(repository, jwtService);
        inOrder.verify(repository).save(user);
        inOrder.verify(jwtService).issue("u-1", "john@example.com", "John", "CUSTOMER");
    }

    @Test
    void signIn_doesNotIssueJwtOnInvalidPassword() {
        User user = user("u-1", "john@example.com");
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", user.getPasswordHash())).thenReturn(false);
        when(repository.save(user)).thenReturn(user);

        assertThrows(InvalidCredentialsException.class,
                () -> userService.signIn(request("john@example.com", "bad")));

        verifyNoInteractions(jwtService);
    }

    @Test
    void signIn_doesNotIssueJwtWhenLocked() {
        User user = user("u-1", "john@example.com");
        user.setLockedUntil(Instant.now().plusSeconds(120));
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        assertThrows(AccountLockedException.class,
                () -> userService.signIn(request("john@example.com", "bad")));

        verifyNoInteractions(jwtService, passwordEncoder);
    }

    @Test
    void requireById_returnsUserWhenFound() {
        User user = user("u-1", "john@example.com");
        when(repository.findById("u-1")).thenReturn(Optional.of(user));

        User result = userService.requireById("u-1");

        assertEquals(user, result);
    }

    @Test
    void requireById_throwsInvalidCredentialsWhenMissing() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> userService.requireById("missing"));
    }

    @Test
    void signIn_resetsExpiredLockBeforeSuccessfulLogin() {
        User user = user("u-1", "john@example.com");
        user.setFirstName("John");
        user.setLockedUntil(Instant.now().minusSeconds(5));
        user.setFailedLoginAttempts(2);

        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", user.getPasswordHash())).thenReturn(true);
        when(repository.save(user)).thenReturn(user);
        when(jwtService.issue("u-1", "john@example.com", "John", "CUSTOMER"))
                .thenReturn(new JwtService.IssuedToken("jwt", Instant.now().plusSeconds(3600)));

        userService.signIn(request("john@example.com", "good"));

        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }

    @Test
    void signIn_persistsMutatedUserOnFailedAttempt() {
        User user = user("u-1", "john@example.com");
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", user.getPasswordHash())).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(InvalidCredentialsException.class,
                () -> userService.signIn(request("john@example.com", "bad")));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertEquals(1, captor.getValue().getFailedLoginAttempts());
    }

    @Test
    void signIn_usesTrimmedEmailButOriginalPassword() {
        User user = user("u-1", "john@example.com");
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("  raw password  ", user.getPasswordHash())).thenReturn(false);
        when(repository.save(user)).thenReturn(user);

        assertThrows(InvalidCredentialsException.class,
                () -> userService.signIn(request("  JOHN@example.com  ", "  raw password  ")));

        verify(passwordEncoder).matches("  raw password  ", user.getPasswordHash());
    }

    @Test
    void signIn_retryAfterIsAtLeastOneSecond() {
        User user = user("u-1", "john@example.com");
        user.setLockedUntil(Instant.now().plusMillis(100));
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        AccountLockedException ex = assertThrows(AccountLockedException.class,
                () -> userService.signIn(request("john@example.com", "pw")));

        assertEquals(1, ex.getRetryAfterSeconds());
    }

    private static SignInRequestDTO request(String email, String password) {
        SignInRequestDTO req = new SignInRequestDTO();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private static User user(String id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("encoded");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole(Role.CUSTOMER);
        return user;
    }
}
