package com.hospital.booking.dto;
import com.hospital.booking.entity.Appointment;
import lombok.*;
import java.time.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AppointmentResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private Appointment.Status status;
    private Appointment.AppointmentType appointmentType;
    private String symptoms;
    private String notes;
    private String diagnosis;
    private String prescription;
    private LocalDate followUpDate;
    private Double consultationFee;
    private Boolean isPaid;
    private String paymentId;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
}
