package com.hospital.booking.dto;
import com.hospital.booking.entity.Payment;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String orderId;
    private String cashfreeOrderId;
    private String paymentSessionId;
    private String paymentLink;
    private Double amount;
    private String currency;
    private Payment.PaymentType paymentType;
    private Payment.Status status;
    private String description;
    private Long appointmentId;
    private String subscriptionPlan;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
