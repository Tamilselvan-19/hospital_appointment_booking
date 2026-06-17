package com.hospital.booking.dto;
import com.hospital.booking.entity.Appointment;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AppointmentRequest {
    @NotNull private Long doctorId;
    @NotNull private LocalDate appointmentDate;
    @NotNull private LocalTime appointmentTime;
    private Appointment.AppointmentType appointmentType = Appointment.AppointmentType.REGULAR;
    private String symptoms;
    private String notes;
}
