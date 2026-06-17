package com.hospital.booking.dto;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DoctorResponse {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phoneNumber;
    private String specialization;
    private String qualification;
    private Integer experienceYears;
    private Double consultationFee;
    private String about;
    private String licenseNumber;
    private Set<String> availableDays;
    private Set<String> timeSlots;
    private Double rating;
    private Integer totalReviews;
    private Boolean isVerified;
    private Boolean isPremium;
    private LocalDateTime premiumExpiryDate;
    private String profileImage;
    private String department;
    private String roomNumber;
    private LocalDateTime createdAt;
}
