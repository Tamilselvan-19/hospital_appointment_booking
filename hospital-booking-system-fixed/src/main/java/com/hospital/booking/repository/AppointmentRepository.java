package com.hospital.booking.repository;
import com.hospital.booking.entity.Appointment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.*;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByDoctorId(Long doctorId);
    List<Appointment> findByStatus(Appointment.Status status);
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id=:did AND a.appointmentDate=:date AND a.status NOT IN ('CANCELLED','NO_SHOW')") List<Appointment> findActiveByDoctorAndDate(@Param("did") Long did, @Param("date") LocalDate date);
    @Query("SELECT a FROM Appointment a WHERE a.patient.id=:pid AND a.appointmentDate >= :date") List<Appointment> findUpcomingByPatient(@Param("pid") Long pid, @Param("date") LocalDate date);
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id=:did AND a.appointmentDate >= :date") List<Appointment> findUpcomingByDoctor(@Param("did") Long did, @Param("date") LocalDate date);
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id=:did AND a.appointmentDate=:date AND a.appointmentTime=:time AND a.status NOT IN ('CANCELLED','NO_SHOW')") Long countByDoctorAndDateTime(@Param("did") Long did, @Param("date") LocalDate date, @Param("time") LocalTime time);
    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate=:date AND a.status='CONFIRMED' AND a.reminderSent=false") List<Appointment> findForReminder(@Param("date") LocalDate date);
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status=:s") Long countByStatus(@Param("s") Appointment.Status s);
}
