package com.hospital.booking.service;

import com.hospital.booking.dto.*;
import com.hospital.booking.entity.*;
import com.hospital.booking.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service @Slf4j
public class PaymentService {

    @Value("${cashfree.app-id:}") private String cashfreeAppId;
    @Value("${cashfree.secret-key:}") private String cashfreeSecretKey;

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AppointmentService appointmentService;
    @Autowired private DoctorService doctorService;
    @Autowired private PatientService patientService;

    @Transactional
    public PaymentResponse createPaymentOrder(Long userId, PaymentRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String orderId = "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
        String idempotencyKey = UUID.randomUUID().toString();

        Payment p = new Payment();
        p.setOrderId(orderId); p.setUser(user);
        p.setAmount(req.getAmount());
        // BUG FIX: original getCurrency() returned "" so currency was blank
        p.setCurrency(req.getCurrency());
        p.setPaymentType(req.getPaymentType()); p.setDescription(req.getDescription());
        p.setAppointmentId(req.getAppointmentId()); p.setSubscriptionPlan(req.getSubscriptionPlan());
        p.setSubscriptionDurationDays(req.getSubscriptionDurationDays());
        p.setStatus(Payment.Status.PENDING); p.setIdempotencyKey(idempotencyKey);
        Payment saved = paymentRepository.save(p);

        saved.setStatus(Payment.Status.INITIATED);
        paymentRepository.save(saved);

        log.info("Payment order created: {} for user: {}", orderId, userId);
        return PaymentResponse.builder()
            .id(saved.getId()).orderId(orderId).cashfreeOrderId(orderId)
            .paymentSessionId("session_" + orderId)
            .paymentLink("http://localhost:8080/api/payments/verify/" + orderId)
            .amount(req.getAmount()).currency(req.getCurrency())
            .paymentType(req.getPaymentType()).status(Payment.Status.INITIATED)
            .description(req.getDescription()).appointmentId(req.getAppointmentId())
            .subscriptionPlan(req.getSubscriptionPlan()).createdAt(saved.getCreatedAt())
            .build();
    }

    @Transactional
    public PaymentResponse verifyPayment(String orderId) {
        Payment p = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new RuntimeException("Payment not found: " + orderId));
        p.setStatus(Payment.Status.SUCCESS); p.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(p);
        processSuccessfulPayment(p);
        return map(p);
    }

    @Transactional
    public void handleWebhook(String payload) {
        log.info("Received Cashfree webhook ({})", payload.length());
        // TODO: verify webhook signature, parse payload, update payment status
    }

    private void processSuccessfulPayment(Payment p) {
        switch (p.getPaymentType()) {
            case APPOINTMENT -> {
                if (p.getAppointmentId() != null)
                    appointmentService.updatePaymentStatus(p.getAppointmentId(), p.getOrderId(), true);
            }
            case SUBSCRIPTION, PREMIUM_UPGRADE -> {
                int days = p.getSubscriptionDurationDays() != null ? p.getSubscriptionDurationDays() : 30;
                User u = p.getUser();
                if (u.getRole() == User.Role.DOCTOR)  doctorService.upgradeToPremium(u.getId(), days);
                else if (u.getRole() == User.Role.PATIENT) patientService.upgradeToPremium(u.getId(), days);
            }
        }
        log.info("Payment processed: {}", p.getOrderId());
    }

    public PaymentResponse getPaymentById(Long id) {
        return map(paymentRepository.findById(id).orElseThrow(() -> new RuntimeException("Payment not found: " + id)));
    }

    public List<PaymentResponse> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserId(userId).stream().map(this::map).collect(Collectors.toList());
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    public Double getTotalRevenue() {
        Double r = paymentRepository.getTotalRevenue();
        return r != null ? r : 0.0;
    }

    private PaymentResponse map(Payment p) {
        return PaymentResponse.builder()
            .id(p.getId()).orderId(p.getOrderId()).cashfreeOrderId(p.getCashfreeOrderId())
            .amount(p.getAmount()).currency(p.getCurrency()).paymentType(p.getPaymentType())
            .status(p.getStatus()).description(p.getDescription()).appointmentId(p.getAppointmentId())
            .subscriptionPlan(p.getSubscriptionPlan())
            .createdAt(p.getCreatedAt()).completedAt(p.getCompletedAt()).build();
    }
}
