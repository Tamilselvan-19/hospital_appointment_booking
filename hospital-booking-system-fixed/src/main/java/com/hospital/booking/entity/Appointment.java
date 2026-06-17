package com.hospital.booking.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.*;

import java.time.*;

@Entity
@Table(name = "appointments")
@Data @NoArgsConstructor @AllArgsConstructor
public class Appointment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @NotNull @Column(name = "appointment_date", nullable = false) private LocalDate appointmentDate;
    @NotNull @Column(name = "appointment_time", nullable = false) private LocalTime appointmentTime;

    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.PENDING;
    @Enumerated(EnumType.STRING) @Column(name = "appointment_type") private AppointmentType appointmentType = AppointmentType.REGULAR;

    @Column(name = "symptoms", length = 1000) private String symptoms;
    @Column(name = "notes", length = 1000) private String notes;
    @Column(name = "diagnosis", length = 1000) private String diagnosis;
    @Column(name = "prescription", length = 2000) private String prescription;
    @Column(name = "follow_up_date") private LocalDate followUpDate;
    @Column(name = "consultation_fee") private Double consultationFee;
    @Column(name = "is_paid") private Boolean isPaid = false;
    @Column(name = "payment_id") private String paymentId;
    @Column(name = "cancellation_reason") private String cancellationReason;
    @Column(name = "reminder_sent") private Boolean reminderSent = false;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "cancelled_at") private LocalDateTime cancelledAt;

    public enum Status { PENDING, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW }
    public enum AppointmentType { REGULAR, FOLLOW_UP, EMERGENCY, PREMIUM }
}
