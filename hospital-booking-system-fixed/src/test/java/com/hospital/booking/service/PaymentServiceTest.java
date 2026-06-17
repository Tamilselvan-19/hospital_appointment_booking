package com.hospital.booking.service;

import com.hospital.booking.dto.PaymentRequest;
import com.hospital.booking.dto.PaymentResponse;
import com.hospital.booking.entity.*;
import com.hospital.booking.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * =====================================================================
 * UNIT TEST SUITE — PaymentService
 * =====================================================================
 * Techniques:
 *  - Mockito  : PaymentRepository, UserRepository, AppointmentService mocked
 *  - BVA      : payment amount boundary (0, negative, positive)
 *  - EP       : APPOINTMENT vs SUBSCRIPTION payment types
 *  - Regression: BUG-005 — currency blank fix
 *  - Functional: order creation, verification flow
 * =====================================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock PaymentRepository    paymentRepo;
    @Mock UserRepository       userRepo;
    @Mock AppointmentService   appointmentService;
    @Mock DoctorService        doctorService;
    @Mock PatientService       patientService;

    @InjectMocks
    PaymentService service;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Pay Patient");
        testUser.setEmail("pay@test.com");
        testUser.setRole(User.Role.PATIENT);
        testUser.setIsActive(true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Functional: Payment Order Creation
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Functional: Create Payment Order")
    class CreatePaymentOrderTests {

        @Test
        @DisplayName("FUNC-01: Valid appointment payment creates order with INITIATED status")
        void createOrder_validAppointmentPayment_initiated() {
            PaymentRequest req = buildReq(500.0, "INR", Payment.PaymentType.APPOINTMENT, 10L);
            Payment savedPayment = mockPayment(req, Payment.Status.INITIATED);

            when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRepo.save(any())).thenReturn(savedPayment);

            PaymentResponse resp = service.createPaymentOrder(1L, req);

            assertNotNull(resp);
            assertNotNull(resp.getOrderId());
            assertTrue(resp.getOrderId().startsWith("ORD"));
            assertEquals(Payment.Status.INITIATED, resp.getStatus());
            assertEquals(500.0, resp.getAmount());
        }

        @Test
        @DisplayName("FUNC-02: Valid subscription payment creates order")
        void createOrder_subscriptionPayment_success() {
            PaymentRequest req = buildReq(999.0, "INR", Payment.PaymentType.SUBSCRIPTION, null);
            req.setSubscriptionPlan("PREMIUM"); req.setSubscriptionDurationDays(30);
            Payment savedPayment = mockPayment(req, Payment.Status.INITIATED);

            when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRepo.save(any())).thenReturn(savedPayment);

            PaymentResponse resp = service.createPaymentOrder(1L, req);
            assertNotNull(resp);
            assertEquals("PREMIUM", resp.getSubscriptionPlan());
        }

        @Test
        @DisplayName("FUNC-03: Non-existent user throws RuntimeException")
        void createOrder_unknownUser_throws() {
            when(userRepo.findById(99L)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class,
                () -> service.createPaymentOrder(99L, buildReq(500.0, "INR", Payment.PaymentType.APPOINTMENT, 1L)));
        }

        @Test
        @DisplayName("REG-BUG005: Currency is correctly stored (not blank)")
        void bug005_currencyNotBlank() {
            PaymentRequest req = buildReq(500.0, "INR", Payment.PaymentType.APPOINTMENT, 1L);
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            Payment saved = mockPayment(req, Payment.Status.INITIATED);

            when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRepo.save(captor.capture())).thenReturn(saved);

            service.createPaymentOrder(1L, req);

            // Verify captured payment has non-blank currency (BUG-005 regression)
            Payment captured = captor.getAllValues().get(0);
            assertNotNull(captured.getCurrency());
            assertFalse(captured.getCurrency().isBlank(), "BUG-005: Currency must not be blank");
            assertEquals("INR", captured.getCurrency());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BVA: Payment Amount
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("BVA: Payment Amount Boundaries")
    class PaymentAmountBVA {

        @ParameterizedTest(name = "Amount {0} → order created")
        @ValueSource(doubles = {0.01, 1.0, 100.0, 500.0, 9999.99})
        @DisplayName("BVA: Positive amounts accepted")
        void bva_positiveAmounts_accepted(double amount) {
            PaymentRequest req = buildReq(amount, "INR", Payment.PaymentType.APPOINTMENT, 1L);
            Payment saved = mockPayment(req, Payment.Status.INITIATED);

            when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRepo.save(any())).thenReturn(saved);

            assertDoesNotThrow(() -> service.createPaymentOrder(1L, req));
        }

        @Test
        @DisplayName("BVA: Zero amount — still creates order (validation at controller level)")
        void bva_zeroAmount_createsOrder() {
            PaymentRequest req = buildReq(0.0, "INR", Payment.PaymentType.APPOINTMENT, 1L);
            Payment saved = mockPayment(req, Payment.Status.INITIATED);

            when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRepo.save(any())).thenReturn(saved);

            assertDoesNotThrow(() -> service.createPaymentOrder(1L, req));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EP: Payment Types
    // ═══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("EP: Payment Type Partitions")
    class PaymentTypeEP {

        @ParameterizedTest(name = "PaymentType.{0} → valid")
        @org.junit.jupiter.params.provider.EnumSource(Payment.PaymentType.class)
        @DisplayName("EP: All payment types accepted")
        void ep_allPaymentTypes_accepted(Payment.PaymentType type) {
            PaymentRequest req = buildReq(300.0, "INR", type, type == Payment.PaymentType.APPOINTMENT ? 1L : null);
            Payment saved = mockPayment(req, Payment.Status.INITIATED);

            when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
            when(paymentRepo.save(any())).thenReturn(saved);

            assertDoesNotThrow(() -> service.createPaymentOrder(1L, req));
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private PaymentRequest buildReq(double amount, String currency,
                                    Payment.PaymentType type, Long appointmentId) {
        PaymentRequest r = new PaymentRequest();
        r.setAmount(amount);
        r.setCurrency(currency);
        r.setPaymentType(type);
        r.setAppointmentId(appointmentId);
        r.setDescription("Test payment");
        return r;
    }

    private Payment mockPayment(PaymentRequest req, Payment.Status status) {
        Payment p = new Payment();
        p.setId(1L);
        p.setOrderId("ORD_TEST_001");
        p.setUser(testUser);
        p.setAmount(req.getAmount());
        p.setCurrency(req.getCurrency());
        p.setPaymentType(req.getPaymentType());
        p.setAppointmentId(req.getAppointmentId());
        p.setSubscriptionPlan(req.getSubscriptionPlan());
        p.setStatus(status);
        return p;
    }
}
