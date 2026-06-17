package com.hospital.booking.controller;

import com.hospital.booking.dto.*;
import com.hospital.booking.entity.Patient;
import com.hospital.booking.security.UserDetailsImpl;
import com.hospital.booking.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:8080","http://localhost:3000"}, maxAge = 3600)
public class PatientController {
    @Autowired private PatientService patientService;

    @GetMapping("/patient/profile") @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PatientResponse>> profile(@AuthenticationPrincipal UserDetailsImpl u) {
        return ResponseEntity.ok(ApiResponse.success(patientService.getPatientByUserId(u.getId())));
    }
    @PutMapping("/patient/profile") @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PatientResponse>> update(@AuthenticationPrincipal UserDetailsImpl u, @RequestBody Patient upd) {
        return ResponseEntity.ok(ApiResponse.success("Updated", patientService.updatePatient(u.getId(), upd)));
    }
}
