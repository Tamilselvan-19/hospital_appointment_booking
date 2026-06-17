package com.hospital.booking.repository;
import com.hospital.booking.entity.Doctor;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);
    List<Doctor> findBySpecialization(String spec);
    List<Doctor> findByDepartment(String dept);
    List<Doctor> findByIsVerified(Boolean v);
    List<Doctor> findByIsPremium(Boolean p);
    @Query("SELECT d FROM Doctor d WHERE d.isVerified=true AND d.user.isActive=true") List<Doctor> findAllActiveVerifiedDoctors();
    @Query("SELECT d FROM Doctor d WHERE d.specialization LIKE %:kw% OR d.user.name LIKE %:kw%") List<Doctor> searchDoctors(@Param("kw") String kw);
    @Query("SELECT COUNT(d) FROM Doctor d WHERE d.isVerified=true") Long countVerifiedDoctors();
}
