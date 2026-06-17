package com.hospital.booking.controller;

import com.hospital.booking.dto.*;
import com.hospital.booking.entity.Doctor;
import com.hospital.booking.security.UserDetailsImpl;
import com.hospital.booking.service.DoctorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController @RequestMapping("/api") @Slf4j
@CrossOrigin(origins = {"http://localhost:8080","http://localhost:3000"}, maxAge = 3600)
public class DoctorController {

    @Autowired private DoctorService doctorService;

    @GetMapping("/doctors") public ResponseEntity<ApiResponse<List<DoctorResponse>>> all() {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getAllDoctors()));
    }
    @GetMapping("/doctors/{id}") public ResponseEntity<ApiResponse<DoctorResponse>> byId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getDoctorById(id)));
    }
    @GetMapping("/doctors/search") public ResponseEntity<ApiResponse<List<DoctorResponse>>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.searchDoctors(keyword)));
    }
    @GetMapping("/doctors/specialization/{s}") public ResponseEntity<ApiResponse<List<DoctorResponse>>> bySpec(@PathVariable String s) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getDoctorsBySpecialization(s)));
    }
    @GetMapping("/doctors/department/{d}") public ResponseEntity<ApiResponse<List<DoctorResponse>>> byDept(@PathVariable String d) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getDoctorsByDepartment(d)));
    }
    @GetMapping("/doctors/specializations") public ResponseEntity<ApiResponse<List<String>>> specializations() {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getAllSpecializations()));
    }
    @GetMapping("/doctors/departments") public ResponseEntity<ApiResponse<List<String>>> departments() {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getAllDepartments()));
    }
    @GetMapping("/doctors/premium") public ResponseEntity<ApiResponse<List<DoctorResponse>>> premium() {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getPremiumDoctors()));
    }
    @GetMapping("/doctor/profile") @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<DoctorResponse>> profile(@AuthenticationPrincipal UserDetailsImpl u) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getDoctorByUserId(u.getId())));
    }
    @PutMapping("/doctor/profile") @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateProfile(@AuthenticationPrincipal UserDetailsImpl u, @RequestBody Doctor upd) {
        return ResponseEntity.ok(ApiResponse.success("Updated", doctorService.updateDoctor(u.getId(), upd)));
    }
    /**
     * BUG FIX: availableDays/timeSlots were required @RequestParam Set<String>.
     * If a doctor unchecked all days or removed all slots, Spring received zero
     * values for that param and threw MissingServletRequestParameterException
     * ("failed to do" error). Now defaults to an empty set so saving with
     * zero days/slots works correctly.
     */
    @PutMapping("/doctor/availability") @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<DoctorResponse>> availability(@AuthenticationPrincipal UserDetailsImpl u,
            @RequestParam(required = false, defaultValue = "") Set<String> availableDays,
            @RequestParam(required = false, defaultValue = "") Set<String> timeSlots) {
        availableDays.remove("");
        timeSlots.remove("");
        return ResponseEntity.ok(ApiResponse.success("Updated", doctorService.updateAvailability(u.getId(), availableDays, timeSlots)));
    }
}
