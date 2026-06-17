package com.hospital.booking.controller;

import com.hospital.booking.dto.*;
import com.hospital.booking.entity.Appointment;
import com.hospital.booking.security.UserDetailsImpl;
import com.hospital.booking.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:8080","http://localhost:3000"}, maxAge = 3600)
@Slf4j
public class AppointmentController {

    @Autowired private AppointmentService appointmentService;

    // ── Patient ───────────────────────────────────────────────────────────────

    @PostMapping("/appointments")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> book(
            @AuthenticationPrincipal UserDetailsImpl u,
            @Valid @RequestBody AppointmentRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Appointment booked",
            appointmentService.bookAppointment(u.getId(), req)));
    }

    /** Upcoming appointments for logged-in patient */
    @GetMapping("/patient/appointments")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getPatientUpcoming(
            @AuthenticationPrincipal UserDetailsImpl u) {
        return ResponseEntity.ok(ApiResponse.success(
            appointmentService.getUpcomingAppointmentsByPatient(u.getId())));
    }

    /** All appointments (history) for logged-in patient */
    @GetMapping("/patient/appointments/all")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAllPatientAppointments(
            @AuthenticationPrincipal UserDetailsImpl u) {
        return ResponseEntity.ok(ApiResponse.success(
            appointmentService.getAppointmentsByPatientUserId(u.getId())));
    }

    /**
     * BUG FIX: This endpoint was missing entirely.
     * The patient "My Appointments" template called GET /api/patient/appointments/{id}
     * but no such route existed → every "View Details" click returned 404.
     */
    @GetMapping("/patient/appointments/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getPatientAppointmentById(
            @AuthenticationPrincipal UserDetailsImpl u,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            appointmentService.getAppointmentByIdForPatient(id, u.getId())));
    }

    /**
     * BUG FIX: Template used POST but backend had PUT. Standardised to PUT.
     * The reason param has a default so frontend doesn't have to send it.
     */
    @PutMapping("/appointments/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancel(
            @AuthenticationPrincipal UserDetailsImpl u,
            @PathVariable Long id,
            @RequestParam(defaultValue = "Cancelled by user") String reason) {
        return ResponseEntity.ok(ApiResponse.success("Cancelled",
            appointmentService.cancelAppointment(id, reason, u.getId())));
    }

    // ── Doctor ────────────────────────────────────────────────────────────────

    @GetMapping("/doctor/appointments")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getDoctorUpcoming(
            @AuthenticationPrincipal UserDetailsImpl u) {
        return ResponseEntity.ok(ApiResponse.success(
            appointmentService.getUpcomingAppointmentsByDoctor(u.getId())));
    }

    @GetMapping("/doctor/appointments/all")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAllDoctorAppointments(
            @AuthenticationPrincipal UserDetailsImpl u) {
        return ResponseEntity.ok(ApiResponse.success(
            appointmentService.getAppointmentsByDoctorUserId(u.getId())));
    }

    @PutMapping("/appointments/{id}/confirm")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Confirmed", appointmentService.confirmAppointment(id)));
    }

    @PutMapping("/appointments/{id}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> complete(
            @PathVariable Long id,
            @RequestParam(required = false) String diagnosis,
            @RequestParam(required = false) String prescription,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate followUpDate) {
        return ResponseEntity.ok(ApiResponse.success("Completed",
            appointmentService.completeAppointment(id, diagnosis, prescription, followUpDate)));
    }

    @PutMapping("/appointments/{id}/no-show")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> noShow(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Marked no-show", appointmentService.markNoShow(id)));
    }

    // ── Availability ──────────────────────────────────────────────────────────

    @GetMapping("/doctors/{doctorId}/available-slots")
    public ResponseEntity<ApiResponse<List<String>>> availableSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAvailableSlots(doctorId, date)));
    }

    @GetMapping("/doctors/{doctorId}/slot-available")
    public ResponseEntity<ApiResponse<Boolean>> slotAvailable(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String time) {
        return ResponseEntity.ok(ApiResponse.success(
            appointmentService.isSlotAvailable(doctorId, date, java.time.LocalTime.parse(time))));
    }

}
