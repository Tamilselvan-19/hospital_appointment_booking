package com.hospital.booking.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;

import java.time.*;

@Entity
@Table(name = "patients")
@Data @NoArgsConstructor @AllArgsConstructor
public class Patient {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "date_of_birth") private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender") private Gender gender;

    @Column(name = "blood_group") private String bloodGroup;
    @Column(name = "address", length = 500) private String address;
    @Column(name = "emergency_contact_name") private String emergencyContactName;
    @Column(name = "emergency_contact_phone") private String emergencyContactPhone;
    @Column(name = "medical_history", length = 2000) private String medicalHistory;
    @Column(name = "allergies") private String allergies;
    @Column(name = "current_medications") private String currentMedications;
    @Column(name = "is_premium") private Boolean isPremium = false;
    @Column(name = "premium_expiry_date") private LocalDateTime premiumExpiryDate;
    @Column(name = "profile_image") private String profileImage;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;

    public enum Gender { MALE, FEMALE, OTHER }
}
