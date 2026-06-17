package com.hospital.booking.service;

import com.hospital.booking.dto.*;
import com.hospital.booking.entity.*;
import com.hospital.booking.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service @Slf4j
public class AppointmentService {

    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private UserRepository userRepository;

    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    public AppointmentResponse getAppointmentById(Long id) {
        return map(appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Appointment not found: " + id)));
    }

    /** BUG FIX: Added ownership check — patients can only view their own appointments */
    public AppointmentResponse getAppointmentByIdForPatient(Long aptId, Long userId) {
        Appointment a = appointmentRepository.findById(aptId)
            .orElseThrow(() -> new RuntimeException("Appointment not found: " + aptId));
        if (!a.getPatient().getUser().getId().equals(userId))
            throw new RuntimeException("Access denied: not your appointment");
        return map(a);
    }

    public List<AppointmentResponse> getAppointmentsByPatientUserId(Long userId) {
        Patient p = patientRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Patient not found"));
        return appointmentRepository.findByPatientId(p.getId()).stream().map(this::map).collect(Collectors.toList());
    }

    public List<AppointmentResponse> getAppointmentsByDoctorUserId(Long userId) {
        Doctor d = doctorRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Doctor not found"));
        return appointmentRepository.findByDoctorId(d.getId()).stream().map(this::map).collect(Collectors.toList());
    }

    public List<AppointmentResponse> getUpcomingAppointmentsByPatient(Long userId) {
        Patient p = patientRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Patient not found"));
        return appointmentRepository.findUpcomingByPatient(p.getId(), LocalDate.now())
            .stream().map(this::map).collect(Collectors.toList());
    }

    public List<AppointmentResponse> getUpcomingAppointmentsByDoctor(Long userId) {
        Doctor d = doctorRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Doctor not found"));
        return appointmentRepository.findUpcomingByDoctor(d.getId(), LocalDate.now())
            .stream().map(this::map).collect(Collectors.toList());
    }

    public List<AppointmentResponse> getAppointmentsByStatus(Appointment.Status status) {
        return appointmentRepository.findByStatus(status).stream().map(this::map).collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponse bookAppointment(Long patientUserId, AppointmentRequest req) {
        Patient patient = patientRepository.findByUserId(patientUserId)
            .orElseThrow(() -> new RuntimeException("Patient not found"));
        Doctor doctor = doctorRepository.findById(req.getDoctorId())
            .orElseThrow(() -> new RuntimeException("Doctor not found"));

        // BUG FIX: Validate appointment date is not in the past
        if (req.getAppointmentDate().isBefore(LocalDate.now()))
            throw new RuntimeException("Appointment date cannot be in the past");

        // Validate slot availability
        if (!isSlotAvailable(doctor.getId(), req.getAppointmentDate(), req.getAppointmentTime()))
            throw new RuntimeException("Selected time slot is not available");

        // Validate doctor works on that day
        String day = req.getAppointmentDate().getDayOfWeek().toString();
        if (!doctor.getAvailableDays().contains(day))
            throw new RuntimeException("Doctor is not available on " + day);

        // Validate time slot in doctor's configured slots
        String timeStr = req.getAppointmentTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        if (!doctor.getTimeSlots().contains(timeStr))
            throw new RuntimeException("Selected time is not in doctor's available slots");

        Appointment a = new Appointment();
        a.setPatient(patient); a.setDoctor(doctor);
        a.setAppointmentDate(req.getAppointmentDate());
        a.setAppointmentTime(req.getAppointmentTime());
        a.setStatus(Appointment.Status.PENDING);
        a.setAppointmentType(req.getAppointmentType() != null ? req.getAppointmentType() : Appointment.AppointmentType.REGULAR);
        a.setSymptoms(req.getSymptoms()); a.setNotes(req.getNotes());
        a.setConsultationFee(doctor.getConsultationFee()); a.setIsPaid(false);

        Appointment saved = appointmentRepository.save(a);
        log.info("Appointment booked: {} patient:{} doctor:{}", saved.getId(), patient.getId(), doctor.getId());
        return map(saved);
    }

    @Transactional
    public AppointmentResponse confirmAppointment(Long id) {
        Appointment a = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Appointment not found: " + id));
        if (a.getStatus() != Appointment.Status.PENDING)
            throw new RuntimeException("Only PENDING appointments can be confirmed");
        a.setStatus(Appointment.Status.CONFIRMED);
        return map(appointmentRepository.save(a));
    }

    @Transactional
    public AppointmentResponse completeAppointment(Long id, String diagnosis, String prescription, LocalDate followUpDate) {
        Appointment a = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Appointment not found: " + id));
        a.setStatus(Appointment.Status.COMPLETED);
        a.setDiagnosis(diagnosis); a.setPrescription(prescription);
        a.setFollowUpDate(followUpDate); a.setCompletedAt(LocalDateTime.now());
        return map(appointmentRepository.save(a));
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long id, String reason, Long userId) {
        Appointment a = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Appointment not found: " + id));
        // Verify the user exists (still throws if userId is invalid)
        if (!userRepository.existsById(userId))
            throw new RuntimeException("User not found: " + userId);

        boolean authorized = a.getPatient().getUser().getId().equals(userId)
            || a.getDoctor().getUser().getId().equals(userId);
        if (!authorized) throw new RuntimeException("Not authorized to cancel this appointment");

        if (a.getStatus() == Appointment.Status.COMPLETED)
            throw new RuntimeException("Completed appointments cannot be cancelled");

        a.setStatus(Appointment.Status.CANCELLED);
        a.setCancellationReason(reason); a.setCancelledAt(LocalDateTime.now());
        log.info("Appointment {} cancelled by user {}", id, userId);
        return map(appointmentRepository.save(a));
    }

    @Transactional
    public AppointmentResponse markNoShow(Long id) {
        Appointment a = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Appointment not found: " + id));
        a.setStatus(Appointment.Status.NO_SHOW);
        return map(appointmentRepository.save(a));
    }

    public boolean isSlotAvailable(Long doctorId, LocalDate date, LocalTime time) {
        return appointmentRepository.countByDoctorAndDateTime(doctorId, date, time) == 0;
    }

    public List<String> getAvailableSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new RuntimeException("Doctor not found"));
        String day = date.getDayOfWeek().toString();
        if (!doctor.getAvailableDays().contains(day)) return List.of();

        List<String> booked = appointmentRepository.findActiveByDoctorAndDate(doctorId, date)
            .stream().map(a -> a.getAppointmentTime().format(DateTimeFormatter.ofPattern("HH:mm")))
            .collect(Collectors.toList());

        return doctor.getTimeSlots().stream()
            .filter(s -> !booked.contains(s)).sorted().collect(Collectors.toList());
    }

    @Transactional
    public void updatePaymentStatus(Long aptId, String paymentId, boolean isPaid) {
        Appointment a = appointmentRepository.findById(aptId)
            .orElseThrow(() -> new RuntimeException("Appointment not found: " + aptId));
        a.setPaymentId(paymentId); a.setIsPaid(isPaid);
        appointmentRepository.save(a);
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void sendAppointmentReminders() {
        appointmentRepository.findForReminder(LocalDate.now().plusDays(1)).forEach(a -> {
            // TODO: integrate email/SMS
            a.setReminderSent(true);
            appointmentRepository.save(a);
            log.info("Reminder sent for appointment: {}", a.getId());
        });
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void markNoShows() {
        // BUG FIX: Removed unused 'yesterday' variable that caused warning
        appointmentRepository.findByStatus(Appointment.Status.CONFIRMED).forEach(a -> {
            if (a.getAppointmentDate().isBefore(LocalDate.now())) {
                a.setStatus(Appointment.Status.NO_SHOW);
                appointmentRepository.save(a);
                log.info("Marked no-show: {}", a.getId());
            }
        });
    }

    private AppointmentResponse map(Appointment a) {
        return AppointmentResponse.builder()
            .id(a.getId())
            .patientId(a.getPatient().getId()).patientName(a.getPatient().getUser().getName())
            .patientPhone(a.getPatient().getUser().getPhoneNumber())
            .doctorId(a.getDoctor().getId()).doctorName(a.getDoctor().getUser().getName())
            .doctorSpecialization(a.getDoctor().getSpecialization())
            .appointmentDate(a.getAppointmentDate()).appointmentTime(a.getAppointmentTime())
            .status(a.getStatus()).appointmentType(a.getAppointmentType())
            .symptoms(a.getSymptoms()).notes(a.getNotes()).diagnosis(a.getDiagnosis())
            .prescription(a.getPrescription()).followUpDate(a.getFollowUpDate())
            .consultationFee(a.getConsultationFee()).isPaid(a.getIsPaid()).paymentId(a.getPaymentId())
            .cancellationReason(a.getCancellationReason())
            .createdAt(a.getCreatedAt()).completedAt(a.getCompletedAt()).cancelledAt(a.getCancelledAt())
            .build();
    }
}
