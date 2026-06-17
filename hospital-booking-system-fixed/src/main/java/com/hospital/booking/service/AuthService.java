package com.hospital.booking.service;

import com.hospital.booking.dto.*;
import com.hospital.booking.entity.*;
import com.hospital.booking.repository.*;
import com.hospital.booking.security.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service @Slf4j
public class AuthService {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserRepository userRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    @Transactional
    public JwtResponse authenticateUser(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        String jwt = jwtUtils.generateJwtToken(auth);
        String refresh = jwtUtils.generateRefreshToken(req.getEmail(), userDetails.getId());
        List<String> perms = userDetails.getAuthorities().stream()
            .map(a -> a.getAuthority()).collect(Collectors.toList());

        // Update last login timestamp
        userRepository.findByEmail(userDetails.getEmail()).ifPresent(u -> {
            u.setLastLogin(LocalDateTime.now());
            userRepository.save(u);
        });

        return JwtResponse.builder()
            .token(jwt).refreshToken(refresh).type("Bearer")
            .id(userDetails.getId()).name(userDetails.getName())
            .email(userDetails.getEmail()).role(userDetails.getRole())
            .permissions(perms).build();
    }

    @Transactional
    public ApiResponse<String> registerUser(SignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            return ApiResponse.error("Email is already registered");
        if (userRepository.existsByPhoneNumber(req.getPhoneNumber()))
            return ApiResponse.error("Phone number is already registered");

        User user = new User();
        user.setName(req.getName()); user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPhoneNumber(req.getPhoneNumber());
        user.setRole(req.getRole() != null ? req.getRole() : User.Role.PATIENT);
        user.setIsActive(true); user.setEmailVerified(false);
        User saved = userRepository.save(user);

        if (saved.getRole() == User.Role.DOCTOR) createDoctorProfile(saved, req);
        else if (saved.getRole() == User.Role.PATIENT) createPatientProfile(saved, req);

        log.info("User registered: {}", saved.getEmail());
        return ApiResponse.success("Registration successful! Please login.");
    }

    private void createDoctorProfile(User user, SignupRequest req) {
        Doctor d = new Doctor();
        d.setUser(user);
        d.setSpecialization(req.getSpecialization() != null ? req.getSpecialization() : "General");
        d.setQualification(req.getQualification() != null ? req.getQualification() : "MBBS");
        d.setExperienceYears(req.getExperienceYears() != null ? req.getExperienceYears() : 0);
        d.setConsultationFee(req.getConsultationFee() != null ? req.getConsultationFee() : 0.0);
        d.setLicenseNumber(req.getLicenseNumber()); d.setDepartment(req.getDepartment());
        // BUG FIX: With the Admin role removed, there is no one left to call the
        // verification endpoint, so doctors must be auto-verified on registration
        // or they would never appear in the public /api/doctors listing.
        d.setIsVerified(true); d.setIsPremium(false); d.setRating(0.0); d.setTotalReviews(0);
        d.setAvailableDays(new HashSet<>(Set.of("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY")));
        d.setTimeSlots(new HashSet<>(Set.of("09:00","10:00","11:00","14:00","15:00","16:00")));
        doctorRepository.save(d);
    }

    private void createPatientProfile(User user, SignupRequest req) {
        Patient p = new Patient();
        p.setUser(user);
        if (req.getDateOfBirth() != null && !req.getDateOfBirth().isBlank())
            p.setDateOfBirth(LocalDate.parse(req.getDateOfBirth(), DateTimeFormatter.ISO_DATE));
        if (req.getGender() != null && !req.getGender().isBlank()) {
            try { p.setGender(Patient.Gender.valueOf(req.getGender().toUpperCase())); }
            catch (IllegalArgumentException ignored) {}
        }
        p.setBloodGroup(req.getBloodGroup()); p.setAddress(req.getAddress());
        p.setEmergencyContactName(req.getEmergencyContactName());
        p.setEmergencyContactPhone(req.getEmergencyContactPhone());
        p.setIsPremium(false);
        patientRepository.save(p);
    }

    public JwtResponse refreshToken(String refreshToken) {
        if (!jwtUtils.validateJwtToken(refreshToken))
            throw new RuntimeException("Invalid refresh token");
        String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
        Long userId = jwtUtils.getUserIdFromJwtToken(refreshToken);
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return JwtResponse.builder()
            .token(jwtUtils.generateTokenFromUsername(username, userId))
            .refreshToken(jwtUtils.generateRefreshToken(username, userId))
            .type("Bearer").id(user.getId()).name(user.getName())
            .email(user.getEmail()).role(user.getRole()).build();
    }

    @Transactional
    public ApiResponse<String> changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword()))
            return ApiResponse.error("Old password is incorrect");
        // BUG FIX: validate new password length server-side
        if (newPassword == null || newPassword.length() < 6)
            return ApiResponse.error("New password must be at least 6 characters");
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ApiResponse.success("Password changed successfully");
    }
}
