package com.hospital.booking.dto;
import com.hospital.booking.entity.Payment;
import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class PaymentRequest {
    @NotNull @Positive private Double amount;
    @NotNull private Payment.PaymentType paymentType;
    private String description;
    private Long appointmentId;
    private String subscriptionPlan;
    private Integer subscriptionDurationDays;
    private String returnUrl;
    /** BUG FIX: original getCurrency() returned "" — now properly defaults to INR */
    private String currency = "INR";
    public String getCurrency() { return (currency == null || currency.isBlank()) ? "INR" : currency; }
}
