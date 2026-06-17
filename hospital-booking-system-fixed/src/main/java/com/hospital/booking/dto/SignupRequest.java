package com.hospital.booking.dto;
import com.hospital.booking.entity.User;
import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignupRequest {
    @NotBlank @Size(min=2,max=100) private String name;
    @NotBlank @Email private String email;
    @NotBlank @Size(min=6) private String password;
    @NotBlank @Size(min=10,max=15) private String phoneNumber;
    private User.Role role = User.Role.PATIENT;
    // Doctor fields
    private String specialization;
    private String qualification;
    private Integer experienceYears;
    private Double consultationFee;
    private String licenseNumber;
    private String department;
    // Patient fields
    private String dateOfBirth;
    private String gender;
    private String bloodGroup;
    private String address;
    private String emergencyContactName;
    private String emergencyContactPhone;
}
