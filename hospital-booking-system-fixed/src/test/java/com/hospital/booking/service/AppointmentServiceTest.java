package com.hospital.booking.service;

import com.hospital.booking.dto.AppointmentRequest;
import com.hospital.booking.dto.AppointmentResponse;
import com.hospital.booking.entity.*;
import com.hospital.booking.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * =====================================================================
 * UNIT TEST SUITE — AppointmentService
 * =====================================================================
 * Techniques applied:
 *  - Mockito    : all repository dependencies mocked; no DB needed
 *  - JUnit 5    : @Test, @ParameterizedTest, @BeforeEach, nested suites
 *  - BVA        : boundary dates (today, yesterday, tomorrow, far future)
 *  - EP         : valid slot / invalid slot / unknown doctor / unknown patient
 *  - Smoke      : happy-path booking confirms minimal flow end-to-end
 *  - Regression : past-date fix, ownership check, slot-availability guard
 * =====================================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService Unit Tests")
class AppointmentServiceTest {

    // ── Mocks ──────────────────────────────────────────────────────────────
    @Mock AppointmentRepository appointmentRepo;
    @Mock DoctorRepository      doctorRepo;
    @Mock PatientRepository     patientRepo;
    @Mock UserRepository        userRepo;

    @InjectMocks
    AppointmentService service;

    // ── Shared test fixtures ───────────────────────────────────────────────
    private User        patientUser, doctorUser;
    private Patient     patient;
    private Doctor      doctor;
    private Appointment pendingAppointment;

    @BeforeEach
    void setUp() {
        patientUser = new User();
        patientUser.setId(10L);
        patientUser.setName("John Doe");
        patientUser.setEmail("john@test.com");
        patientUser.setRole(User.Role.PATIENT);

        doctorUser = new User();
        doctorUser.setId(20L);
        doctorUser.setName("Dr. Smith");
        doctorUser.setEmail("smith@hospital.com");
        doctorUser.setRole(User.Role.DOCTOR);

        patient = new Patient();
        patient.setId(1L);
        patient.setUser(patientUser);

        doctor = new Doctor();
        doctor.setId(2L);
        doctor.setUser(doctorUser);
        doctor.setSpecialization("Cardiology");
        doctor.setConsultationFee(500.0);
        doctor.setAvailableDays(new HashSet<>(Set.of("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY")));
        doctor.setTimeSlots(new HashSet<>(Set.of("09:00","10:00","11:00","14:00","15:00","16:00")));

        pendingAppointment = new Appointment();
        pendingAppointment.setId(100L);
        pendingAppointment.setPatient(patient);
        pendingAppointment.setDoctor(doctor);
        pendingAppointment.setStatus(Appointment.Status.PENDING);
        pendingAppointment.setAppointmentDate(LocalDate.now().plusDays(2));
        pendingAppointment.setAppointmentTime(LocalTime.of(10, 0));
        pendingAppointment.setConsultationFee(500.0);
        pendingAppointment.setIsPaid(false);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 1. SMOKE TEST — happy-path booking
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Smoke: Book Appointment Happy Path")
    class SmokeTests {

        @Test
        @DisplayName("SMOKE-01: Patient books a valid appointment for next Monday")
        void smokeBookAppointment_success() {
            // Find next Monday dynamically so test never fails due to day-of-week
            LocalDate nextMonday = LocalDate.now().with(
                java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY));

            AppointmentRequest req = new AppointmentRequest();
            req.setDoctorId(2L);
            req.setAppointmentDate(nextMonday);
            req.setAppointmentTime(LocalTime.of(9, 0));
            req.setSymptoms("chest pain");

            when(patientRepo.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(doctorRepo.findById(2L)).thenReturn(Optional.of(doctor));
            when(appointmentRepo.countByDoctorAndDateTime(2L, nextMonday, LocalTime.of(9,0))).thenReturn(0L);
            when(appointmentRepo.save(any())).thenReturn(pendingAppointment);

            AppointmentResponse resp = service.bookAppointment(10L, req);

            assertNotNull(resp);
            assertEquals(100L, resp.getId());
            assertEquals(Appointment.Status.PENDING, resp.getStatus());
            verify(appointmentRepo, times(1)).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. BOUNDARY VALUE ANALYSIS — appointment dates
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("BVA: Appointment Date Boundaries")
    class BoundaryValueTests {

        /**
         * EP Partition for appointmentDate:
         *  INVALID  : date < today  (yesterday, 30-days ago, ...)
         *  BOUNDARY : today itself  (edge — system rejects past, should also reject today if before cutoff)
         *  VALID    : tomorrow, next week, far future
         */

        @Test
        @DisplayName("BVA-01: Yesterday (boundary - 1) → REJECTED")
        void bva_pastDate_yesterday_rejected() {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            AppointmentRequest req = buildReq(yesterday, "09:00");

            when(patientRepo.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(doctorRepo.findById(2L)).thenReturn(Optional.of(doctor));

            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.bookAppointment(10L, req));
            assertTrue(ex.getMessage().contains("past"), "Should reject past date");
        }

        @Test
        @DisplayName("BVA-02: Today (boundary) → REJECTED as past")
        void bva_pastDate_today_rejected() {
            AppointmentRequest req = buildReq(LocalDate.now(), "09:00");

            when(patientRepo.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(doctorRepo.findById(2L)).thenReturn(Optional.of(doctor));

            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.bookAppointment(10L, req));
            assertTrue(ex.getMessage().contains("past"));
        }

        @Test
        @DisplayName("BVA-03: Tomorrow (boundary + 1) → accepted if day matches doctor schedule")
        void bva_tomorrowDate_accepted_ifDoctorAvailable() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            String dayName = tomorrow.getDayOfWeek().toString();
            // Ensure doctor is available tomorrow; override availableDays if needed
            doctor.setAvailableDays(new HashSet<>(Set.of(dayName)));

            AppointmentRequest req = buildReq(tomorrow, "09:00");
            when(patientRepo.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(doctorRepo.findById(2L)).thenReturn(Optional.of(doctor));
            when(appointmentRepo.countByDoctorAndDateTime(2L, tomorrow, LocalTime.of(9,0))).thenReturn(0L);
            when(appointmentRepo.save(any())).thenReturn(pendingAppointment);

            AppointmentResponse resp = service.bookAppointment(10L, req);
            assertNotNull(resp);
        }

        @Test
        @DisplayName("BVA-04: Far future date (1 year ahead) → accepted")
        void bva_farFutureDate_accepted() {
            LocalDate farFuture = LocalDate.now().plusYears(1).with(
                java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY));
            AppointmentRequest req = buildReq(farFuture, "09:00");

            when(patientRepo.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(doctorRepo.findById(2L)).thenReturn(Optional.of(doctor));
            when(appointmentRepo.countByDoctorAndDateTime(eq(2L), eq(farFuture), any())).thenReturn(0L);
            when(appointmentRepo.save(any())).thenReturn(pendingAppointment);

            assertDoesNotThrow(() -> service.bookAppointment(10L, req));
        }

        @Test
        @DisplayName("BVA-05: 30 days ago → REJECTED")
        void bva_thirtyDaysAgo_rejected() {
            LocalDate thirtyAgo = LocalDate.now().minusDays(30);
            AppointmentRequest req = buildReq(thirtyAgo, "09:00");

            when(patientRepo.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(doctorRepo.findById(2L)).thenReturn(Optional.of(doctor));

            assertThrows(RuntimeException.class, () -> service.bookAppointment(10L, req));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. EQUIVALENCE PARTITIONING — time slots
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("EP: Time Slot Partitions")
    class EquivalencePartitioningTests {

        private LocalDate validDate;

        @BeforeEach
        void setupDate() {
            validDate = LocalDate.now().with(
                java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY));
        }

        @ParameterizedTest(name = "EP Valid slot: {0} → accepted")
        @ValueSource(strings = {"09:00", "10:00", "11:00", "14:00", "15:00", "16:00"})
        @DisplayName("EP-01: Valid doctor time slots")
        void ep_validSlots_accepted(String timeStr) {
            AppointmentRequest req = buildReq(validDate, timeStr);
            when(patientRepo.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(doctorRepo.findById(2L)).thenReturn(Optional.of(doctor));
            when(appointmentRepo.countByDoctorAndDateTime(eq(2L), eq(validDate), any())).thenReturn(0L);
            when(appointmentRepo.save(any())).thenReturn(pendingAppointment);

            assertDoesNotThrow(() -> service.bookAppointment(10L, req));
        }

        @ParameterizedTest(name = "EP Invalid slot: {0} → rejected")
        @ValueSource(strings = {"08:00", "12:00", "13:00", "17:00", "00:00", "23:59"})
        @DisplayName("EP-02: Invalid time slots (not in doctor's config)")
        void ep_invalidSlots_rejected(String timeStr) {
            AppointmentRequest req = buildReq(validDate, timeStr);
            when(patientRepo.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(doctorRepo.findById(2L)).thenReturn(Optional.of(doctor));
            when(appointmentRepo.countByDoctorAndDateTime(eq(2L), eq(validDate), any())).thenReturn(0L);

            assertThrows(RuntimeException.class, () -> service.bookAppointment(10L, req));
        }

        @Test
        @DisplayName("EP-03: Already booked slot → rejected (slot unavailable)")
        void ep_alreadyBookedSlot_rejected() {
            AppointmentRequest req = buildReq(validDate, "09:00");
            when(patientRepo.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(doctorRepo.findById(2L)).thenReturn(Optional.of(doctor));
            when(appointmentRepo.countByDoctorAndDateTime(eq(2L), eq(validDate), any())).thenReturn(1L);

            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.bookAppointment(10L, req));
            assertTrue(ex.getMessage().contains("not available"));
        }

        @Test
        @DisplayName("EP-04: Weekend booking → rejected (doctor not available)")
        void ep_weekendBooking_rejected() {
            LocalDate saturday = LocalDate.now().with(
                java.time.temporal.TemporalAdjusters.next(DayOfWeek.SATURDAY));
            AppointmentRequest req = buildReq(saturday, "09:00");
            when(patientRepo.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(doctorRepo.findById(2L)).thenReturn(Optional.of(doctor));
            when(appointmentRepo.countByDoctorAndDateTime(eq(2L), eq(saturday), any())).thenReturn(0L);

            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.bookAppointment(10L, req));
            assertTrue(ex.getMessage().contains("not available on"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. FUNCTIONAL TESTS — confirm / complete / cancel / no-show
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Functional: Appointment Status Transitions")
    class FunctionalStatusTests {

        @Test
        @DisplayName("FUNC-01: Confirm PENDING → CONFIRMED")
        void func_confirmPending_success() {
            when(appointmentRepo.findById(100L)).thenReturn(Optional.of(pendingAppointment));
            when(appointmentRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            AppointmentResponse resp = service.confirmAppointment(100L);
            assertEquals(Appointment.Status.CONFIRMED, resp.getStatus());
        }

        @Test
        @DisplayName("FUNC-02: Confirm non-PENDING → RuntimeException")
        void func_confirmNonPending_throws() {
            pendingAppointment.setStatus(Appointment.Status.CONFIRMED);
            when(appointmentRepo.findById(100L)).thenReturn(Optional.of(pendingAppointment));

            assertThrows(RuntimeException.class, () -> service.confirmAppointment(100L));
        }

        @Test
        @DisplayName("FUNC-03: Cancel by patient (owner) → CANCELLED")
        void func_cancelByPatient_success() {
            when(appointmentRepo.findById(100L)).thenReturn(Optional.of(pendingAppointment));
            when(userRepo.existsById(10L)).thenReturn(true);
            when(appointmentRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            AppointmentResponse resp = service.cancelAppointment(100L, "feeling better", 10L);
            assertEquals(Appointment.Status.CANCELLED, resp.getStatus());
            assertNotNull(resp.getCancellationReason());
        }

        @Test
        @DisplayName("FUNC-04: Cancel by doctor (owner) → CANCELLED")
        void func_cancelByDoctor_success() {
            when(appointmentRepo.findById(100L)).thenReturn(Optional.of(pendingAppointment));
            when(userRepo.existsById(20L)).thenReturn(true);
            when(appointmentRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            AppointmentResponse resp = service.cancelAppointment(100L, "emergency", 20L);
            assertEquals(Appointment.Status.CANCELLED, resp.getStatus());
        }

        @Test
        @DisplayName("FUNC-05: Cancel by unrelated user → RuntimeException (REGRESSION: ownership check)")
        void func_cancelByStranger_throws() {
            when(appointmentRepo.findById(100L)).thenReturn(Optional.of(pendingAppointment));
            when(userRepo.existsById(99L)).thenReturn(true);

            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.cancelAppointment(100L, "reason", 99L));
            assertTrue(ex.getMessage().contains("Not authorized"));
        }

        @Test
        @DisplayName("FUNC-06: Cancel COMPLETED appointment → RuntimeException")
        void func_cancelCompleted_throws() {
            pendingAppointment.setStatus(Appointment.Status.COMPLETED);
            when(appointmentRepo.findById(100L)).thenReturn(Optional.of(pendingAppointment));
            when(userRepo.existsById(10L)).thenReturn(true);

            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.cancelAppointment(100L, "reason", 10L));
            assertTrue(ex.getMessage().contains("cannot be cancelled"));
        }

        @Test
        @DisplayName("FUNC-07: Complete appointment with diagnosis/prescription")
        void func_completeAppointment_success() {
            pendingAppointment.setStatus(Appointment.Status.CONFIRMED);
            when(appointmentRepo.findById(100L)).thenReturn(Optional.of(pendingAppointment));
            when(appointmentRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            AppointmentResponse resp = service.completeAppointment(
                100L, "Hypertension", "Amlodipine 5mg", LocalDate.now().plusMonths(1));
            assertEquals(Appointment.Status.COMPLETED, resp.getStatus());
            assertEquals("Hypertension", resp.getDiagnosis());
        }

        @Test
        @DisplayName("FUNC-08: Mark no-show changes status to NO_SHOW")
        void func_markNoShow_success() {
            when(appointmentRepo.findById(100L)).thenReturn(Optional.of(pendingAppointment));
            when(appointmentRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            AppointmentResponse resp = service.markNoShow(100L);
            assertEquals(Appointment.Status.NO_SHOW, resp.getStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5. REGRESSION TESTS — bugs documented in codebase
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Regression: Bug-Fix Verification")
    class RegressionTests {

        @Test
        @DisplayName("REG-01: Patient cannot view another patient's appointment (ownership bug fix)")
        void reg_ownershipCheck_deniedForStranger() {
            // appointment belongs to patient user id=10, but userId=99 tries to view it
            when(appointmentRepo.findById(100L)).thenReturn(Optional.of(pendingAppointment));
            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getAppointmentByIdForPatient(100L, 99L));
            assertTrue(ex.getMessage().contains("Access denied"));
        }

        @Test
        @DisplayName("REG-02: Patient can view their own appointment")
        void reg_ownershipCheck_allowedForOwner() {
            when(appointmentRepo.findById(100L)).thenReturn(Optional.of(pendingAppointment));
            assertDoesNotThrow(() -> service.getAppointmentByIdForPatient(100L, 10L));
        }

        @Test
        @DisplayName("REG-03: Booking with non-existent doctor throws (not silently fails)")
        void reg_unknownDoctor_throws() {
            AppointmentRequest req = buildReq(LocalDate.now().plusDays(3), "09:00");
            when(patientRepo.findByUserId(10L)).thenReturn(Optional.of(patient));
            when(doctorRepo.findById(2L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.bookAppointment(10L, req));
            assertTrue(ex.getMessage().contains("Doctor not found"));
        }

        @Test
        @DisplayName("REG-04: Booking with non-existent patient throws")
        void reg_unknownPatient_throws() {
            AppointmentRequest req = buildReq(LocalDate.now().plusDays(3), "09:00");
            when(patientRepo.findByUserId(10L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.bookAppointment(10L, req));
            assertTrue(ex.getMessage().contains("Patient not found"));
        }

        @Test
        @DisplayName("REG-05: isSlotAvailable returns false when count > 0")
        void reg_slotAvailability_returnsFalse_whenBooked() {
            when(appointmentRepo.countByDoctorAndDateTime(2L, LocalDate.now().plusDays(1), LocalTime.of(9,0)))
                .thenReturn(1L);
            assertFalse(service.isSlotAvailable(2L, LocalDate.now().plusDays(1), LocalTime.of(9,0)));
        }

        @Test
        @DisplayName("REG-06: isSlotAvailable returns true when slot is free")
        void reg_slotAvailability_returnsTrue_whenFree() {
            when(appointmentRepo.countByDoctorAndDateTime(2L, LocalDate.now().plusDays(1), LocalTime.of(9,0)))
                .thenReturn(0L);
            assertTrue(service.isSlotAvailable(2L, LocalDate.now().plusDays(1), LocalTime.of(9,0)));
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────
    private AppointmentRequest buildReq(LocalDate date, String time) {
        AppointmentRequest r = new AppointmentRequest();
        r.setDoctorId(2L);
        r.setAppointmentDate(date);
        r.setAppointmentTime(LocalTime.parse(time));
        r.setSymptoms("test symptom");
        return r;
    }
}
