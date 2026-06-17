package com.hospital.booking.controller;

import com.hospital.booking.dto.*;
import com.hospital.booking.security.UserDetailsImpl;
import com.hospital.booking.service.PaymentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:8080","http://localhost:3000"}, maxAge = 3600) @Slf4j
public class PaymentController {
    @Autowired private PaymentService paymentService;

    @PostMapping("/payments/create") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> create(@AuthenticationPrincipal UserDetailsImpl u,
            @Valid @RequestBody PaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Order created", paymentService.createPaymentOrder(u.getId(), req)));
    }
    @GetMapping("/payments/verify/{orderId}") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> verify(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success("Verified", paymentService.verifyPayment(orderId)));
    }
    @PostMapping("/payments/webhook/cashfree")
    public ResponseEntity<String> webhook(@RequestBody String payload) {
        log.info("Webhook received"); paymentService.handleWebhook(payload); return ResponseEntity.ok("OK");
    }
    @GetMapping("/payments/my-payments") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> myPayments(@AuthenticationPrincipal UserDetailsImpl u) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentsByUser(u.getId())));
    }
    @GetMapping("/payments/{id}") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> byId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentById(id)));
    }
}
