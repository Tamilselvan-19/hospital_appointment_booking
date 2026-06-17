package com.hospital.booking.dto;
import com.hospital.booking.entity.Patient;
import lombok.*;
import java.time.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PatientResponse {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private Patient.Gender gender;
    private String bloodGroup;
    private String address;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String medicalHistory;
    private String allergies;
    private String currentMedications;
    private Boolean isPremium;
    private LocalDateTime premiumExpiryDate;
    private String profileImage;
    private LocalDateTime createdAt;
}
