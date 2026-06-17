package com.hospital.booking.service;

import com.hospital.booking.dto.*;
import com.hospital.booking.entity.User;
import com.hospital.booking.repository.*;
import com.hospital.booking.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * =====================================================================
 * UNIT TEST SUITE — AuthService
 * =====================================================================
 * Techniques:
 *  - Mockito    : UserRepository, PasswordEncoder, JwtUtils all mocked
 *  - BVA        : password length boundaries (5 chars, 6 chars, 7 chars)
 *  - EP         : duplicate email / duplicate phone / valid new user
 *  - Regression : changePassword new-password-min-length fix
 *  - Functional : login, register, refresh token flows
 * =====================================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock AuthenticationManager authManager;
    @Mock UserRepository        userRepo;
    @Mock DoctorRepository      doctorRepo;
    @Mock PatientRepository     patientRepo;
    @Mock PasswordEncoder       passwordEncoder;
    @Mock JwtUtils              jwtUtils;

    @InjectMocks
    AuthService service;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Alice");
        testUser.setEmail("alice@test.com");
        testUser.setPassword("$2a$encoded");
        testUser.setPhoneNumber("9876543210");
        testUser.setRole(User.Role.PATIENT);
        testUser.setIsActive(true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Registration — EP & Functional
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Registration Tests (EP + Functional)")
    class RegistrationTests {

        @Test
        @DisplayName("REG-01: Valid new patient registration → success message")
        void register_validPatient_success() {
            SignupRequest req = buildSignup("Bob", "bob@test.com", "9000000001");

            when(userRepo.existsByEmail(req.getEmail())).thenReturn(false);
            when(userRepo.existsByPhoneNumber(req.getPhoneNumber())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userRepo.save(any())).thenReturn(testUser);
            when(patientRepo.save(any())).thenReturn(null);

            ApiResponse<String> resp = service.registerUser(req);
            assertTrue(resp.isSuccess());
            assertTrue(resp.getMessage().contains("Registration"));
        }

        @Test
        @DisplayName("REG-02: EP Duplicate email (invalid partition) → error")
        void register_duplicateEmail_error() {
            SignupRequest req = buildSignup("Bob", "alice@test.com", "9000000002");
            when(userRepo.existsByEmail("alice@test.com")).thenReturn(true);

            ApiResponse<String> resp = service.registerUser(req);
            assertFalse(resp.isSuccess());
            assertTrue(resp.getMessage().contains("Email"));
        }

        @Test
        @DisplayName("REG-03: EP Duplicate phone (invalid partition) → error")
        void register_duplicatePhone_error() {
            SignupRequest req = buildSignup("Bob", "bob2@test.com", "9876543210");
            when(userRepo.existsByEmail(any())).thenReturn(false);
            when(userRepo.existsByPhoneNumber("9876543210")).thenReturn(true);

            ApiResponse<String> resp = service.registerUser(req);
            assertFalse(resp.isSuccess());
            assertTrue(resp.getMessage().contains("Phone"));
        }

        @Test
        @DisplayName("REG-04: New user gets role PATIENT when role not specified")
        void register_defaultRoleIsPatient() {
            SignupRequest req = buildSignup("Carol", "carol@test.com", "9111111111");
            req.setRole(null); // not specified

            when(userRepo.existsByEmail(any())).thenReturn(false);
            when(userRepo.existsByPhoneNumber(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("encoded");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepo.save(captor.capture())).thenReturn(testUser);
            when(patientRepo.save(any())).thenReturn(null);

            service.registerUser(req);
            assertEquals(User.Role.PATIENT, captor.getValue().getRole());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Change Password — BVA on password length
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("BVA: Change Password Length Boundaries")
    class ChangePasswordBVA {

        @BeforeEach
        void mockUser() {
            // Use lenient() so tests that short-circuit before reaching these
            // stubs (e.g. password-length validation) don't trigger
            // UnnecessaryStubbingException.
            lenient().when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
            lenient().when(passwordEncoder.matches("oldPass123", "$2a$encoded")).thenReturn(true);
        }

        @Test
        @DisplayName("BVA-01: New password 5 chars (boundary-1) → REJECTED (regression fix)")
        void changePassword_5chars_rejected() {
            ApiResponse<String> resp = service.changePassword(1L, "oldPass123", "12345");
            assertFalse(resp.isSuccess());
            assertTrue(resp.getMessage().contains("6 characters"));
        }

        @Test
        @DisplayName("BVA-02: New password 6 chars (boundary) → ACCEPTED")
        void changePassword_6chars_accepted() {
            when(passwordEncoder.encode("123456")).thenReturn("encoded2");
            when(userRepo.save(any())).thenReturn(testUser);

            ApiResponse<String> resp = service.changePassword(1L, "oldPass123", "123456");
            assertTrue(resp.isSuccess());
        }

        @Test
        @DisplayName("BVA-03: New password 7 chars (boundary+1) → ACCEPTED")
        void changePassword_7chars_accepted() {
            when(passwordEncoder.encode("1234567")).thenReturn("encoded3");
            when(userRepo.save(any())).thenReturn(testUser);

            ApiResponse<String> resp = service.changePassword(1L, "oldPass123", "1234567");
            assertTrue(resp.isSuccess());
        }

        @Test
        @DisplayName("BVA-04: New password null → REJECTED")
        void changePassword_null_rejected() {
            ApiResponse<String> resp = service.changePassword(1L, "oldPass123", null);
            assertFalse(resp.isSuccess());
        }

        @Test
        @DisplayName("BVA-05: Empty string password → REJECTED")
        void changePassword_empty_rejected() {
            ApiResponse<String> resp = service.changePassword(1L, "oldPass123", "");
            assertFalse(resp.isSuccess());
        }

        @Test
        @DisplayName("BVA-06: Wrong old password → REJECTED")
        void changePassword_wrongOldPassword_rejected() {
            when(passwordEncoder.matches("wrongPass", "$2a$encoded")).thenReturn(false);
            ApiResponse<String> resp = service.changePassword(1L, "wrongPass", "newPass123");
            assertFalse(resp.isSuccess());
            assertTrue(resp.getMessage().contains("incorrect"));
        }

        @Test
        @DisplayName("BVA-07: Very long password 100 chars → ACCEPTED (no upper limit)")
        void changePassword_100chars_accepted() {
            String longPass = "a".repeat(100);
            when(passwordEncoder.encode(longPass)).thenReturn("encodedLong");
            when(userRepo.save(any())).thenReturn(testUser);

            ApiResponse<String> resp = service.changePassword(1L, "oldPass123", longPass);
            assertTrue(resp.isSuccess());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Login — Functional
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Functional: Login / JWT")
    class LoginTests {

        @Test
        @DisplayName("LOGIN-01: Valid credentials → JWT token returned")
        void login_validCredentials_returnsJwt() {
            LoginRequest req = new LoginRequest();
            req.setEmail("alice@test.com");
            req.setPassword("password123");

            UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
            when(userDetails.getId()).thenReturn(1L);
            when(userDetails.getName()).thenReturn("Alice");
            when(userDetails.getEmail()).thenReturn("alice@test.com");
            when(userDetails.getRole()).thenReturn(User.Role.PATIENT);
            when(userDetails.getAuthorities()).thenReturn(java.util.Collections.emptyList());

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(authManager.authenticate(any())).thenReturn(auth);
            when(jwtUtils.generateJwtToken(auth)).thenReturn("jwt-token-abc");
            when(jwtUtils.generateRefreshToken(any(), any())).thenReturn("refresh-token-xyz");
            when(userRepo.findByEmail("alice@test.com")).thenReturn(Optional.of(testUser));
            when(userRepo.save(any())).thenReturn(testUser);

            JwtResponse resp = service.authenticateUser(req);
            assertNotNull(resp);
            assertEquals("jwt-token-abc", resp.getToken());
            assertEquals("Bearer", resp.getType());
        }

        @Test
        @DisplayName("LOGIN-02: Invalid credentials → AuthenticationException propagated")
        void login_invalidCredentials_throws() {
            LoginRequest req = new LoginRequest();
            req.setEmail("alice@test.com");
            req.setPassword("wrongpass");

            when(authManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(BadCredentialsException.class, () -> service.authenticateUser(req));
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────
    private SignupRequest buildSignup(String name, String email, String phone) {
        SignupRequest r = new SignupRequest();
        r.setName(name);
        r.setEmail(email);
        r.setPassword("password123");
        r.setPhoneNumber(phone);
        r.setRole(User.Role.PATIENT);
        return r;
    }
}
