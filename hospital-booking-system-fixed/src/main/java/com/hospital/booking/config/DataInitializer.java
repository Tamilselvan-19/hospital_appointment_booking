package com.hospital.booking.config;

import com.hospital.booking.entity.*;
import com.hospital.booking.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component @Slf4j
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            createSampleDoctor();
            createSamplePatient();
            log.info("Data initialization complete.");
        } catch (Exception e) {
            log.error("Data init error: {}", e.getMessage());
        }
    }

    private void createSampleDoctor() {
        if (userRepository.existsByEmail("doctor@hospital.com")) return;
        User u = new User();
        u.setName("Dr. John Smith"); u.setEmail("doctor@hospital.com");
        u.setPassword(passwordEncoder.encode("doctor123"));
        u.setPhoneNumber("8888888888"); u.setRole(User.Role.DOCTOR);
        u.setIsActive(true); u.setEmailVerified(true);
        User saved = userRepository.save(u);

        Doctor d = new Doctor();
        d.setUser(saved); d.setSpecialization("Cardiology");
        d.setQualification("MBBS, MD (Cardiology)"); d.setExperienceYears(15);
        d.setConsultationFee(500.0); d.setAbout("Experienced cardiologist");
        d.setLicenseNumber("MCI12345"); d.setDepartment("Cardiology"); d.setRoomNumber("101");
        d.setIsVerified(true); d.setIsPremium(false); d.setRating(4.5); d.setTotalReviews(10);
        d.setAvailableDays(new HashSet<>(Set.of("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY")));
        d.setTimeSlots(new HashSet<>(Set.of("09:00","10:00","11:00","14:00","15:00","16:00")));
        doctorRepository.save(d);
    }

    private void createSamplePatient() {
        if (userRepository.existsByEmail("patient@hospital.com")) return;
        User u = new User();
        u.setName("Jane Doe"); u.setEmail("patient@hospital.com");
        u.setPassword(passwordEncoder.encode("patient123"));
        u.setPhoneNumber("7777777777"); u.setRole(User.Role.PATIENT);
        u.setIsActive(true); u.setEmailVerified(true);
        User saved = userRepository.save(u);

        Patient p = new Patient();
        p.setUser(saved); p.setBloodGroup("O+");
        p.setAddress("123 Main Street"); p.setEmergencyContactName("Emergency Contact");
        p.setEmergencyContactPhone("6666666666"); p.setIsPremium(false);
        patientRepository.save(p);
    }
}
