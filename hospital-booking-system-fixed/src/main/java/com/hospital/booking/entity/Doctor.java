package com.hospital.booking.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "doctors")
@Data @NoArgsConstructor @AllArgsConstructor
public class Doctor {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotBlank @Column(nullable = false)
    private String specialization;

    @NotBlank @Column(nullable = false)
    private String qualification;

    @Column(name = "experience_years")
    @Positive
    private Integer experienceYears;

    @Column(name = "consultation_fee")
    @Positive
    private Double consultationFee;

    @Column(name = "about", length = 2000)
    private String about;

    @Column(name = "license_number", unique = true)
    private String licenseNumber;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "doctor_availability", joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "available_day")
    private Set<String> availableDays = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "doctor_time_slots", joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "time_slot")
    private Set<String> timeSlots = new HashSet<>();

    @Column(name = "rating") private Double rating = 0.0;
    @Column(name = "total_reviews") private Integer totalReviews = 0;
    @Column(name = "is_verified") private Boolean isVerified = false;
    @Column(name = "is_premium") private Boolean isPremium = false;
    @Column(name = "premium_expiry_date") private LocalDateTime premiumExpiryDate;
    @Column(name = "profile_image") private String profileImage;
    @Column(name = "department") private String department;
    @Column(name = "room_number") private String roomNumber;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
