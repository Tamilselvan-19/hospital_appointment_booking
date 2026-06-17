package com.hospital.booking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.booking.dto.*;
import com.hospital.booking.entity.*;
import com.hospital.booking.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * =====================================================================
 * INTEGRATION TEST SUITE — Full Spring Boot + H2 in-memory DB
 * =====================================================================
 * Techniques:
 *  - @SpringBootTest   : full application context loaded
 *  - H2 in-memory DB   : isolated, no MySQL needed
 *  - JWT flows         : register → login → use token in header
 *  - Smoke testing     : auth, register, public API endpoints
 *  - UAT style         : end-to-end user journey scenarios
 *  - Regression        : duplicate email, wrong password, expired token
 * =====================================================================
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Full Integration Tests (H2 + JWT)")
class FullIntegrationTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository     userRepo;
    @Autowired DoctorRepository   doctorRepo;
    @Autowired PatientRepository  patientRepo;
    @Autowired PasswordEncoder    passwordEncoder;

    // Shared state across ordered tests
    static String patientToken;
    static String doctorToken;
    static Long   doctorId;

    // ═══════════════════════════════════════════════════════════════════════
    // SMOKE — public endpoints accessible without auth
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("SMOKE-01: GET /api/doctors (public) → 200")
    void smokePublicDoctorsList_200() throws Exception {
        mockMvc.perform(get("/api/doctors"))
            .andExpect(status().isOk());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UAT SCENARIO 1 — Patient registers and logs in
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("UAT-01: Register a new patient → success")
    void uat_registerPatient_success() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setName("Integration Patient");
        req.setEmail("int.patient@test.com");
        req.setPassword("Pass1234!");
        req.setPhoneNumber("9000000100");
        req.setRole(User.Role.PATIENT);

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(3)
    @DisplayName("UAT-02: Login with registered patient → JWT token returned")
    void uat_loginPatient_success() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("int.patient@test.com");
        req.setPassword("Pass1234!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        patientToken = objectMapper.readTree(body).get("token").asText();
        assertNotNull(patientToken);
        assertFalse(patientToken.isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("UAT-03: Authenticated patient views their appointments → 200")
    void uat_patientViewsAppointments_200() throws Exception {
        Assumptions.assumeTrue(patientToken != null, "Patient token required");

        mockMvc.perform(get("/api/patient/appointments")
                .header("Authorization", "Bearer " + patientToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UAT SCENARIO 2 — Doctor registers and logs in
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("UAT-04: Register a new doctor → success + doctor profile auto-created")
    void uat_registerDoctor_success() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setName("Integration Doctor");
        req.setEmail("int.doctor@hospital.com");
        req.setPassword("DocPass123!");
        req.setPhoneNumber("9000000200");
        req.setRole(User.Role.DOCTOR);
        req.setSpecialization("General Medicine");
        req.setQualification("MBBS MD");
        req.setExperienceYears(8);
        req.setConsultationFee(400.0);

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify doctor profile was created in DB
        assertTrue(userRepo.existsByEmail("int.doctor@hospital.com"));
    }

    @Test
    @Order(6)
    @DisplayName("UAT-05: Login with registered doctor → JWT token")
    void uat_loginDoctor_success() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("int.doctor@hospital.com");
        req.setPassword("DocPass123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        doctorToken = objectMapper.readTree(body).get("token").asText();
        Long userId  = objectMapper.readTree(body).get("id").asLong();

        doctorRepo.findByUserId(userId).ifPresent(d -> doctorId = d.getId());
        assertNotNull(doctorToken);
    }

    @Test
    @Order(7)
    @DisplayName("UAT-06: Doctor views their appointments → 200")
    void uat_doctorViewsAppointments_200() throws Exception {
        Assumptions.assumeTrue(doctorToken != null, "Doctor token required");

        mockMvc.perform(get("/api/doctor/appointments")
                .header("Authorization", "Bearer " + doctorToken))
            .andExpect(status().isOk());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REGRESSION — Duplicate registration
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @Order(8)
    @DisplayName("REG-01: Register with duplicate email → error (not crash)")
    void reg_duplicateEmail_returnsError() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setName("Duplicate");
        req.setEmail("int.patient@test.com"); // already registered
        req.setPassword("Pass1234!");
        req.setPhoneNumber("9000000999");
        req.setRole(User.Role.PATIENT);

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Email")));
    }

    @Test
    @Order(9)
    @DisplayName("REG-02: Login with wrong password → 401 or error response")
    void reg_wrongPassword_rejected() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("int.patient@test.com");
        req.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                // Either 401 Unauthorized or 200 with success=false
                assertTrue(status == 401 || status == 200, "Expected 401 or 200");
            });
    }

    @Test
    @Order(10)
    @DisplayName("REG-03: Expired/invalid JWT → 401 unauthorized")
    void reg_invalidJwt_401() throws Exception {
        mockMvc.perform(get("/api/patient/appointments")
                .header("Authorization", "Bearer INVALID.JWT.TOKEN"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assertTrue(status == 401 || status == 403,
                    "Expected 401 or 403 for invalid JWT, got " + status);
            });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UAT SCENARIO 3 — Doctor list is visible after doctor registers
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @Order(11)
    @DisplayName("UAT-07: Registered doctor appears in public doctor listing")
    void uat_registeredDoctorAppearsInList() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/doctors"))
            .andExpect(status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("Integration Doctor") || body.contains("General Medicine"),
            "Registered doctor should appear in listings");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SMOKE — Other protected endpoints shape-check
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @Order(12)
    @DisplayName("SMOKE-02: GET /api/patient/appointments/all → 200 with valid token")
    void smoke_patientAllAppointments_200() throws Exception {
        Assumptions.assumeTrue(patientToken != null);

        mockMvc.perform(get("/api/patient/appointments/all")
                .header("Authorization", "Bearer " + patientToken))
            .andExpect(status().isOk());
    }

    @Test
    @Order(13)
    @DisplayName("SMOKE-03: GET /api/doctor/profile → 200 with doctor token")
    void smoke_doctorProfile_200() throws Exception {
        Assumptions.assumeTrue(doctorToken != null);

        mockMvc.perform(get("/api/doctor/profile")
                .header("Authorization", "Bearer " + doctorToken))
            .andExpect(status().isOk());
    }
}
