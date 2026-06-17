package com.hospital.booking.service;

import com.hospital.booking.dto.PatientResponse;
import com.hospital.booking.entity.*;
import com.hospital.booking.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service @Slf4j
public class PatientService {

    @Autowired private PatientRepository patientRepository;
    @Autowired private UserRepository userRepository;

    public List<PatientResponse> getAllPatients() {
        return patientRepository.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    public PatientResponse getPatientById(Long id) {
        return map(patientRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found: " + id)));
    }

    public PatientResponse getPatientByUserId(Long userId) {
        return map(patientRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Patient not found")));
    }

    public Patient getPatientEntityByUserId(Long userId) {
        return patientRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    @Transactional
    public PatientResponse updatePatient(Long userId, Patient upd) {
        Patient p = patientRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Patient not found"));
        User u = p.getUser();

        if (upd.getUser() != null) {
            if (upd.getUser().getName() != null)        u.setName(upd.getUser().getName());
            if (upd.getUser().getPhoneNumber() != null) u.setPhoneNumber(upd.getUser().getPhoneNumber());
            userRepository.save(u);
        }
        if (upd.getDateOfBirth() != null)          p.setDateOfBirth(upd.getDateOfBirth());
        if (upd.getGender() != null)               p.setGender(upd.getGender());
        if (upd.getBloodGroup() != null)           p.setBloodGroup(upd.getBloodGroup());
        if (upd.getAddress() != null)              p.setAddress(upd.getAddress());
        if (upd.getEmergencyContactName() != null) p.setEmergencyContactName(upd.getEmergencyContactName());
        if (upd.getEmergencyContactPhone() != null)p.setEmergencyContactPhone(upd.getEmergencyContactPhone());
        if (upd.getMedicalHistory() != null)       p.setMedicalHistory(upd.getMedicalHistory());
        if (upd.getAllergies() != null)             p.setAllergies(upd.getAllergies());
        if (upd.getCurrentMedications() != null)   p.setCurrentMedications(upd.getCurrentMedications());
        return map(patientRepository.save(p));
    }

    @Transactional
    public void upgradeToPremium(Long userId, int days) {
        Patient p = patientRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Patient not found"));
        p.setIsPremium(true); p.setPremiumExpiryDate(LocalDateTime.now().plusDays(days));
        patientRepository.save(p);
        log.info("Patient upgraded to premium: {}", userId);
    }

    @Transactional
    public void checkAndExpirePremiumSubscriptions() {
        patientRepository.findExpiredPremium(LocalDateTime.now()).forEach(p -> {
            p.setIsPremium(false); p.setPremiumExpiryDate(null);
            patientRepository.save(p);
            log.info("Premium expired for patient: {}", p.getId());
        });
    }

    public List<PatientResponse> searchPatients(String kw) {
        return patientRepository.searchPatients(kw).stream().map(this::map).collect(Collectors.toList());
    }

    public List<PatientResponse> getPremiumPatients() {
        return patientRepository.findByIsPremium(true).stream().map(this::map).collect(Collectors.toList());
    }

    private PatientResponse map(Patient p) {
        User u = p.getUser();
        return PatientResponse.builder()
            .id(p.getId()).userId(u.getId()).name(u.getName()).email(u.getEmail())
            .phoneNumber(u.getPhoneNumber()).dateOfBirth(p.getDateOfBirth()).gender(p.getGender())
            .bloodGroup(p.getBloodGroup()).address(p.getAddress())
            .emergencyContactName(p.getEmergencyContactName())
            .emergencyContactPhone(p.getEmergencyContactPhone())
            .medicalHistory(p.getMedicalHistory()).allergies(p.getAllergies())
            .currentMedications(p.getCurrentMedications()).isPremium(p.getIsPremium())
            .premiumExpiryDate(p.getPremiumExpiryDate()).profileImage(p.getProfileImage())
            .createdAt(p.getCreatedAt()).build();
    }
}
