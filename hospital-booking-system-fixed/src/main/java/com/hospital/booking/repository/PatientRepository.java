package com.hospital.booking.repository;
import com.hospital.booking.entity.Patient;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUserId(Long userId);
    List<Patient> findByIsPremium(Boolean p);
    @Query("SELECT p FROM Patient p WHERE p.isPremium=true AND p.premiumExpiryDate < :d") List<Patient> findExpiredPremium(@Param("d") LocalDateTime d);
    @Query("SELECT p FROM Patient p WHERE p.user.name LIKE %:kw% OR p.user.email LIKE %:kw%") List<Patient> searchPatients(@Param("kw") String kw);
    @Query("SELECT COUNT(p) FROM Patient p WHERE p.isPremium=true") Long countPremiumPatients();
}
