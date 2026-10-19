package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.SignUpRequestDTO;
import com.epam.edp.demo.enums.Role;
import com.epam.edp.demo.exception.EmailAlreadyExistsException;
import com.epam.edp.demo.exception.WeakPasswordException;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceSignUpTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(repository, passwordEncoder, jwtService, 5, 15);
    }

    @Test
    void signUp_assignsCustomerRole_andNormalizesInput() {
        SignUpRequestDTO req = request("  John  ", "  Doe  ", "  John.Doe@Example.COM  ", "Password@123");
        when(repository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed-password");
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userService.signUp(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("John", saved.getFirstName());
        assertEquals("Doe", saved.getLastName());
        assertEquals("john.doe@example.com", saved.getEmail());
        assertEquals("hashed-password", saved.getPasswordHash());
        assertEquals(Role.CUSTOMER, saved.getRole());
        assertEquals(Role.CUSTOMER, created.getRole());
        verify(repository).existsByEmail("john.doe@example.com");
    }

    @Test
    void signUp_usesNormalizedEmailForDuplicateLookup_andExceptionMessage() {
        SignUpRequestDTO req = request("Jane", "Doe", "  Jane.Agent@Example.COM  ", "Password@123");
        when(repository.existsByEmail("jane.agent@example.com")).thenReturn(true);

        EmailAlreadyExistsException ex = assertThrows(EmailAlreadyExistsException.class, () -> userService.signUp(req));

        assertEquals("Email already registered: jane.agent@example.com", ex.getMessage());
        verify(repository).existsByEmail("jane.agent@example.com");
        verify(repository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void signUp_rejectsDuplicateEmail_whenTravelAgentAlreadyPreCreated() {
        SignUpRequestDTO req = request("Jane", "Doe", "agent@example.com", "Password@123");
        when(repository.existsByEmail("agent@example.com")).thenReturn(true);

        EmailAlreadyExistsException ex = assertThrows(EmailAlreadyExistsException.class, () -> userService.signUp(req));

        assertEquals("Email already registered: agent@example.com", ex.getMessage());
        verify(repository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void signUp_rethrowsDuplicateKeyAsEmailAlreadyExists() {
        SignUpRequestDTO req = request("Jane", "Doe", "duplicate@example.com", "Password@123");
        when(repository.existsByEmail("duplicate@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed-password");
        when(repository.save(any(User.class))).thenThrow(new DuplicateKeyException("dup"));

        EmailAlreadyExistsException ex = assertThrows(EmailAlreadyExistsException.class, () -> userService.signUp(req));

        assertEquals("Email already registered: duplicate@example.com", ex.getMessage());
    }

    @Test
    void signUp_rejectsPasswordContainingFirstName_caseInsensitive_beforeAnyPersistence() {
        SignUpRequestDTO req = request("  John  ", "Doe", "johnny@example.com", "safeJOHN#2026");

        WeakPasswordException ex = assertThrows(WeakPasswordException.class, () -> userService.signUp(req));

        assertEquals("Password must not contain your first name", ex.getMessage());
        verify(repository, never()).existsByEmail(anyString());
        verify(repository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void signUp_rejectsPasswordContainingNormalizedEmailLocalPart_caseInsensitive() {
        SignUpRequestDTO req = request("Marta", "Doe", "  Alice.Agent@Example.com  ", "MyALICE.AGENT#2026");

        WeakPasswordException ex = assertThrows(WeakPasswordException.class, () -> userService.signUp(req));

        assertEquals("Password must not contain your email", ex.getMessage());
        verify(repository, never()).existsByEmail(anyString());
        verify(repository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void signUp_passesPasswordToEncoderWithoutTrimmingOrNormalization() {
        String rawPassword = "  Sup3r$ecret Pass  ";
        SignUpRequestDTO req = request("Jane", "Doe", "jane.doe@example.com", rawPassword);
        when(repository.existsByEmail("jane.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn("hashed-password");
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.signUp(req);

        verify(passwordEncoder).encode(rawPassword);
    }

    @Test
    void signUp_trimsNamesButPreservesInternalSpaces() {
        SignUpRequestDTO req = request("  Mary  Ann  ", "  Van  Helsing  ", "mary@example.com", "Safe#2026");
        when(repository.existsByEmail("mary@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Safe#2026")).thenReturn("hashed-password");
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userService.signUp(req);

        assertEquals("Mary  Ann", created.getFirstName());
        assertEquals("Van  Helsing", created.getLastName());
    }

    @Test
    void signUp_returnsRepositorySavedInstanceWithAssignedId() {
        SignUpRequestDTO req = request("Jane", "Doe", "saved@example.com", "Password@123");
        User persisted = new User("Jane", "Doe", "saved@example.com", "hashed-password", Role.CUSTOMER);
        persisted.setId("user-123");
        when(repository.existsByEmail("saved@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed-password");
        when(repository.save(any(User.class))).thenReturn(persisted);

        User created = userService.signUp(req);

        assertSame(persisted, created);
        assertEquals("user-123", created.getId());
    }

    @Test
    void signUp_callsCollaboratorsInExpectedOrderOnSuccess() {
        SignUpRequestDTO req = request("Jane", "Doe", "ordered@example.com", "Password@123");
        when(repository.existsByEmail("ordered@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed-password");
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.signUp(req);

        InOrder inOrder = inOrder(repository, passwordEncoder, repository);
        inOrder.verify(repository).existsByEmail("ordered@example.com");
        inOrder.verify(passwordEncoder).encode("Password@123");
        inOrder.verify(repository).save(any(User.class));
    }

    @Test
    void signUp_doesNotUseJwtService() {
        SignUpRequestDTO req = request("Jane", "Doe", "nojwt@example.com", "Password@123");
        when(repository.existsByEmail("nojwt@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed-password");
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.signUp(req);

        verifyNoInteractions(jwtService);
    }

    @Test
    void signUp_savesEncodedPasswordInsteadOfRawPassword() {
        SignUpRequestDTO req = request("Jane", "Doe", "encoded@example.com", "Password@123");
        when(repository.existsByEmail("encoded@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed-password");
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.signUp(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertEquals("hashed-password", captor.getValue().getPasswordHash());
    }

    @Test
    void signUp_rethrowsDuplicateKeyWithNormalizedEmailMessage() {
        SignUpRequestDTO req = request("Jane", "Doe", "  DUPLICATE@Example.COM  ", "Password@123");
        when(repository.existsByEmail("duplicate@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed-password");
        when(repository.save(any(User.class))).thenThrow(new DuplicateKeyException("dup"));

        EmailAlreadyExistsException ex = assertThrows(EmailAlreadyExistsException.class, () -> userService.signUp(req));

        assertEquals("Email already registered: duplicate@example.com", ex.getMessage());
    }

    @Test
    void signUp_encodesPasswordBeforeTranslatingDuplicateKeyException() {
        SignUpRequestDTO req = request("Jane", "Doe", "retry@example.com", "Password@123");
        when(repository.existsByEmail("retry@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed-password");
        when(repository.save(any(User.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThrows(EmailAlreadyExistsException.class, () -> userService.signUp(req));

        verify(passwordEncoder).encode("Password@123");
        verify(repository).save(any(User.class));
    }

    @Test
    void signUp_rejectsPasswordContainingFirstNameAsSubstringWithinLongerPassword() {
        SignUpRequestDTO req = request("Eve", "Doe", "eve.safe@example.com", "UltraEVE-Guard#2026");

        WeakPasswordException ex = assertThrows(WeakPasswordException.class, () -> userService.signUp(req));

        assertEquals("Password must not contain your first name", ex.getMessage());
        verify(repository, never()).existsByEmail(anyString());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void signUp_rejectsPasswordContainingEmailLocalPartBeforeAtSymbolOnly() {
        SignUpRequestDTO req = request("Jane", "Doe", "traveler@example.com", "UltraTRAVELER#2026");

        WeakPasswordException ex = assertThrows(WeakPasswordException.class, () -> userService.signUp(req));

        assertEquals("Password must not contain your email", ex.getMessage());
        verify(repository, never()).existsByEmail(anyString());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void signUp_rejectsPasswordContainingEmailLocalPartWithPlusAlias() {
        SignUpRequestDTO req = request("Jane", "Doe", "neo+travel@example.com", "SafeNEO+TRAVEL#2026");

        WeakPasswordException ex = assertThrows(WeakPasswordException.class, () -> userService.signUp(req));

        assertEquals("Password must not contain your email", ex.getMessage());
        verify(repository, never()).existsByEmail(anyString());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void signUp_prioritizesFirstNameMessageWhenPasswordContainsFirstNameAndEmail() {
        SignUpRequestDTO req = request("Neo", "Doe", "neo.agent@example.com", "ShieldNEO.AGENT#2026");

        WeakPasswordException ex = assertThrows(WeakPasswordException.class, () -> userService.signUp(req));

        assertEquals("Password must not contain your first name", ex.getMessage());
        verify(repository, never()).existsByEmail(anyString());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void signUp_allowsPasswordContainingOnlyEmailDomain() {
        SignUpRequestDTO req = request("Marta", "Doe", "traveler@example.com", "UseEXAMPLE-Domain#2026");
        when(repository.existsByEmail("traveler@example.com")).thenReturn(false);
        when(passwordEncoder.encode("UseEXAMPLE-Domain#2026")).thenReturn("hashed-password");
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userService.signUp(req);

        assertEquals("traveler@example.com", created.getEmail());
        verify(passwordEncoder).encode("UseEXAMPLE-Domain#2026");
    }

    @Test
    void signUp_allowsPasswordContainingOnlyLastName_currentBehavior() {
        SignUpRequestDTO req = request("Jane", "Doe", "jane.user@example.com", "StrongDOE#2026");
        when(repository.existsByEmail("jane.user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongDOE#2026")).thenReturn("hashed-password");
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userService.signUp(req);

        assertEquals("jane.user@example.com", created.getEmail());
        assertEquals(Role.CUSTOMER, created.getRole());
    }

    @Test
    void signUp_rejectsPasswordContainingWholeEmailWhenAtMissing_currentBehavior() {
        SignUpRequestDTO req = request("Jane", "Doe", "broken-email", "safeBROKEN-EMAIL#2026");

        WeakPasswordException ex = assertThrows(WeakPasswordException.class, () -> userService.signUp(req));

        assertEquals("Password must not contain your email", ex.getMessage());
        verify(repository, never()).existsByEmail(anyString());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void signUp_usesLowercasedTrimmedEmailForDuplicateCheck() {
        SignUpRequestDTO req = request("Jane", "Doe", "  MIXED.CASE@Example.Com  ", "Strong#2026");
        when(repository.existsByEmail("mixed.case@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.signUp(req));

        verify(repository).existsByEmail("mixed.case@example.com");
    }

    private static SignUpRequestDTO request(String firstName, String lastName, String email, String password) {
        SignUpRequestDTO req = new SignUpRequestDTO();
        req.setFirstName(firstName);
        req.setLastName(lastName);
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }
}

