package com.hospital.booking.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data @NoArgsConstructor @AllArgsConstructor
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_id", unique = true, nullable = false) private String orderId;
    @Column(name = "cashfree_order_id") private String cashfreeOrderId;
    @Column(name = "payment_id") private String paymentId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount", nullable = false) private Double amount;
    @Column(name = "currency", nullable = false) private String currency = "INR";

    @Enumerated(EnumType.STRING) @Column(name = "payment_type", nullable = false) private PaymentType paymentType;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.PENDING;

    @Column(name = "description") private String description;
    @Column(name = "appointment_id") private Long appointmentId;
    @Column(name = "subscription_plan") private String subscriptionPlan;
    @Column(name = "subscription_duration_days") private Integer subscriptionDurationDays;
    @Column(name = "idempotency_key", unique = true) private String idempotencyKey;
    @Column(name = "failure_reason") private String failureReason;
    @Column(name = "refund_id") private String refundId;
    @Column(name = "refund_amount") private Double refundAmount;
    @Column(name = "refund_status") private String refundStatus;
    @Column(name = "webhook_response", length = 2000) private String webhookResponse;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "webhook_received_at") private LocalDateTime webhookReceivedAt;

    public enum Status { PENDING, INITIATED, SUCCESS, FAILED, CANCELLED, REFUNDED }
    public enum PaymentType { APPOINTMENT, SUBSCRIPTION, PREMIUM_UPGRADE }
}
