package com.hospital.booking.service;

import com.hospital.booking.dto.DoctorResponse;
import com.hospital.booking.entity.*;
import com.hospital.booking.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service @Slf4j
public class DoctorService {

    @Autowired private DoctorRepository doctorRepository;
    @Autowired private UserRepository userRepository;

    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAllActiveVerifiedDoctors().stream().map(this::map).collect(Collectors.toList());
    }

    public List<DoctorResponse> getAllDoctorsIncludingUnverified() {
        return doctorRepository.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    public DoctorResponse getDoctorById(Long id) {
        return map(doctorRepository.findById(id).orElseThrow(() -> new RuntimeException("Doctor not found: " + id)));
    }

    public DoctorResponse getDoctorByUserId(Long userId) {
        return map(doctorRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Doctor not found")));
    }

    public List<DoctorResponse> getDoctorsBySpecialization(String s) {
        return doctorRepository.findBySpecialization(s).stream().map(this::map).collect(Collectors.toList());
    }

    public List<DoctorResponse> getDoctorsByDepartment(String d) {
        return doctorRepository.findByDepartment(d).stream().map(this::map).collect(Collectors.toList());
    }

    public List<DoctorResponse> searchDoctors(String kw) {
        return doctorRepository.searchDoctors(kw).stream().map(this::map).collect(Collectors.toList());
    }

    public List<DoctorResponse> getPremiumDoctors() {
        return doctorRepository.findByIsPremium(true).stream().map(this::map).collect(Collectors.toList());
    }

    public List<String> getAllSpecializations() {
        return doctorRepository.findAll().stream().map(Doctor::getSpecialization).distinct().sorted().collect(Collectors.toList());
    }

    public List<String> getAllDepartments() {
        return doctorRepository.findAll().stream().map(Doctor::getDepartment)
            .filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
    }

    @Transactional
    public DoctorResponse updateDoctor(Long userId, Doctor upd) {
        Doctor d = doctorRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Doctor not found"));
        if (upd.getSpecialization() != null) d.setSpecialization(upd.getSpecialization());
        if (upd.getQualification() != null)  d.setQualification(upd.getQualification());
        if (upd.getExperienceYears() != null) d.setExperienceYears(upd.getExperienceYears());
        if (upd.getConsultationFee() != null) d.setConsultationFee(upd.getConsultationFee());
        if (upd.getAbout() != null)          d.setAbout(upd.getAbout());
        if (upd.getDepartment() != null)     d.setDepartment(upd.getDepartment());
        if (upd.getRoomNumber() != null)     d.setRoomNumber(upd.getRoomNumber());
        if (upd.getAvailableDays() != null)  d.setAvailableDays(upd.getAvailableDays());
        if (upd.getTimeSlots() != null)      d.setTimeSlots(upd.getTimeSlots());
        return map(doctorRepository.save(d));
    }

    @Transactional
    public DoctorResponse updateAvailability(Long userId, Set<String> days, Set<String> slots) {
        Doctor d = doctorRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Doctor not found"));
        if (days != null)  d.setAvailableDays(days);
        if (slots != null) d.setTimeSlots(slots);
        return map(doctorRepository.save(d));
    }

    @Transactional
    public void verifyDoctor(Long doctorId) {
        Doctor d = doctorRepository.findById(doctorId).orElseThrow(() -> new RuntimeException("Doctor not found"));
        d.setIsVerified(true); doctorRepository.save(d);
        log.info("Doctor verified: {}", doctorId);
    }

    @Transactional
    public void upgradeToPremium(Long userId, int days) {
        Doctor d = doctorRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Doctor not found"));
        d.setIsPremium(true); d.setPremiumExpiryDate(LocalDateTime.now().plusDays(days));
        doctorRepository.save(d);
        log.info("Doctor upgraded to premium: {}", userId);
    }

    @Transactional
    public void updateRating(Long doctorId, Double newRating) {
        Doctor d = doctorRepository.findById(doctorId).orElseThrow(() -> new RuntimeException("Doctor not found"));
        int total = d.getTotalReviews() + 1;
        d.setRating(((d.getRating() * d.getTotalReviews()) + newRating) / total);
        d.setTotalReviews(total);
        doctorRepository.save(d);
    }

    private DoctorResponse map(Doctor d) {
        User u = d.getUser();
        return DoctorResponse.builder()
            .id(d.getId()).userId(u.getId()).name(u.getName()).email(u.getEmail())
            .phoneNumber(u.getPhoneNumber()).specialization(d.getSpecialization())
            .qualification(d.getQualification()).experienceYears(d.getExperienceYears())
            .consultationFee(d.getConsultationFee()).about(d.getAbout())
            .licenseNumber(d.getLicenseNumber()).availableDays(d.getAvailableDays())
            .timeSlots(d.getTimeSlots()).rating(d.getRating()).totalReviews(d.getTotalReviews())
            .isVerified(d.getIsVerified()).isPremium(d.getIsPremium())
            .premiumExpiryDate(d.getPremiumExpiryDate()).profileImage(d.getProfileImage())
            .department(d.getDepartment()).roomNumber(d.getRoomNumber()).createdAt(d.getCreatedAt())
            .build();
    }
}
