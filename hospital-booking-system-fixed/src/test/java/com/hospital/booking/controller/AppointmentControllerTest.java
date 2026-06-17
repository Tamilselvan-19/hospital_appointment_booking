package com.hospital.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.booking.config.SecurityConfig;
import com.hospital.booking.dto.*;
import com.hospital.booking.entity.*;
import com.hospital.booking.security.*;
import com.hospital.booking.service.AppointmentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * =====================================================================
 * INTEGRATION TEST SUITE — AppointmentController (MockMvc / @WebMvcTest)
 * =====================================================================
 * Techniques:
 *  - @WebMvcTest   : loads only the web layer (controller + security)
 *  - MockMvc       : HTTP-level request/response testing without server
 *  - @MockBean     : AppointmentService isolated from infrastructure
 *  - @WithMockUser : simulates authenticated PATIENT / DOCTOR roles
 *  - EP            : valid vs invalid request bodies
 *  - Smoke         : key endpoints return HTTP 200
 *  - Security      : unauthorized access returns 401/403
 * =====================================================================
 */
@WebMvcTest(AppointmentController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("AppointmentController Integration Tests (MockMvc)")
class AppointmentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AppointmentService appointmentService;
    // Security infrastructure beans needed by @WebMvcTest + SecurityConfig
    @MockBean UserDetailsServiceImpl userDetailsService;
    @MockBean JwtUtils jwtUtils;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    private AppointmentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = AppointmentResponse.builder()
            .id(1L)
            .patientId(10L).patientName("John Doe")
            .doctorId(2L).doctorName("Dr. Smith").doctorSpecialization("Cardiology")
            .appointmentDate(LocalDate.now().plusDays(3))
            .appointmentTime(LocalTime.of(10, 0))
            .status(Appointment.Status.PENDING)
            .appointmentType(Appointment.AppointmentType.REGULAR)
            .consultationFee(500.0).isPaid(false)
            .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SMOKE TESTS
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Smoke: Core Endpoints Reachable")
    class SmokeTests {

        @Test
        @DisplayName("SMOKE-01: GET /api/patient/appointments → 200 for authenticated PATIENT")
        @WithMockUser(roles = "PATIENT", username = "john@test.com")
        void smokeGetPatientAppointments_200() throws Exception {
            when(appointmentService.getUpcomingAppointmentsByPatient(any())).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/patient/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("SMOKE-02: GET /api/doctor/appointments → 200 for authenticated DOCTOR")
        @WithMockUser(roles = "DOCTOR", username = "smith@hospital.com")
        void smokeGetDoctorAppointments_200() throws Exception {
            when(appointmentService.getUpcomingAppointmentsByDoctor(any())).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/doctor/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("SMOKE-03: GET /api/appointments/{id}/slots → 200 publicly accessible")
        @WithMockUser(roles = "PATIENT")
        void smokeGetAvailableSlots_200() throws Exception {
            when(appointmentService.getAvailableSlots(any(), any()))
                .thenReturn(List.of("09:00", "10:00", "11:00"));

            mockMvc.perform(get("/api/doctors/2/available-slots")
                    .param("date", LocalDate.now().plusDays(3).toString()))
                .andExpect(status().isOk());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECURITY TESTS
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Security: Access Control")
    class SecurityTests {

        @Test
        @DisplayName("SEC-01: Unauthenticated access to patient endpoint → 401/302")
        void secUnauthenticated_patientEndpoint() throws Exception {
            mockMvc.perform(get("/api/patient/appointments"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Spring Security redirects to login (302) or returns 401
                    assertTrue(status == 401 || status == 302 || status == 403,
                        "Expected 401/302/403 but got " + status);
                });
        }

        @Test
        @DisplayName("SEC-02: DOCTOR role cannot POST book appointment (patient-only)")
        @WithMockUser(roles = "DOCTOR")
        void secDoctorCannotBookAppointment() throws Exception {
            AppointmentRequest req = new AppointmentRequest();
            req.setDoctorId(2L);
            req.setAppointmentDate(LocalDate.now().plusDays(3));
            req.setAppointmentTime(LocalTime.of(10, 0));

            mockMvc.perform(post("/api/appointments")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("SEC-03: PATIENT role cannot confirm appointment (doctor-only)")
        @WithMockUser(roles = "PATIENT")
        void secPatientCannotConfirmAppointment() throws Exception {
            mockMvc.perform(put("/api/appointments/1/confirm")
                    .with(csrf()))
                .andExpect(status().isForbidden());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FUNCTIONAL — Book Appointment
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Functional: Book Appointment")
    class BookAppointmentTests {

        @Test
        @DisplayName("FUNC-01: Valid book request → 200 with appointment response")
        @WithMockUser(roles = "PATIENT", username = "john@test.com")
        void bookAppointment_validRequest_200() throws Exception {
            AppointmentRequest req = new AppointmentRequest();
            req.setDoctorId(2L);
            req.setAppointmentDate(LocalDate.now().plusDays(3));
            req.setAppointmentTime(LocalTime.of(10, 0));
            req.setSymptoms("chest pain");

            when(appointmentService.bookAppointment(any(), any())).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/appointments")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @DisplayName("FUNC-02: Service throws RuntimeException → 500 returned")
        @WithMockUser(roles = "PATIENT", username = "john@test.com")
        void bookAppointment_serviceThrows_500() throws Exception {
            AppointmentRequest req = new AppointmentRequest();
            req.setDoctorId(2L);
            req.setAppointmentDate(LocalDate.now().minusDays(1));
            req.setAppointmentTime(LocalTime.of(10, 0));

            when(appointmentService.bookAppointment(any(), any()))
                .thenThrow(new RuntimeException("Appointment date cannot be in the past"));

            mockMvc.perform(post("/api/appointments")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is5xxServerError());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FUNCTIONAL — Cancel Appointment
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Functional: Cancel Appointment")
    class CancelAppointmentTests {

        @Test
        @DisplayName("FUNC-03: Patient cancels own appointment → 200")
        @WithMockUser(roles = "PATIENT", username = "john@test.com")
        void cancelAppointment_patient_200() throws Exception {
            sampleResponse.setStatus(Appointment.Status.CANCELLED);
            when(appointmentService.cancelAppointment(eq(1L), anyString(), any()))
                .thenReturn(sampleResponse);

            mockMvc.perform(put("/api/appointments/1/cancel")
                    .with(csrf())
                    .param("reason", "feeling better"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FUNCTIONAL — Doctor Actions
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Functional: Doctor Appointment Actions")
    class DoctorActionTests {

        @Test
        @DisplayName("FUNC-04: Doctor confirms appointment → 200")
        @WithMockUser(roles = "DOCTOR", username = "smith@hospital.com")
        void confirmAppointment_doctor_200() throws Exception {
            sampleResponse.setStatus(Appointment.Status.CONFIRMED);
            when(appointmentService.confirmAppointment(1L)).thenReturn(sampleResponse);

            mockMvc.perform(put("/api/appointments/1/confirm").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("FUNC-05: Doctor marks no-show → 200")
        @WithMockUser(roles = "DOCTOR", username = "smith@hospital.com")
        void markNoShow_doctor_200() throws Exception {
            sampleResponse.setStatus(Appointment.Status.NO_SHOW);
            when(appointmentService.markNoShow(1L)).thenReturn(sampleResponse);

            mockMvc.perform(put("/api/appointments/1/no-show").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NO_SHOW"));
        }
    }
}
