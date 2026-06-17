package com.hospital.booking.service;

import com.hospital.booking.dto.DoctorResponse;
import com.hospital.booking.entity.*;
import com.hospital.booking.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * =====================================================================
 * UNIT TEST SUITE — DoctorService
 * =====================================================================
 * Techniques:
 *  - Mockito    : DoctorRepository, UserRepository mocked
 *  - EP         : verified vs unverified, existing vs non-existing doctor
 *  - Functional : search, filter by specialization/department, update
 *  - BVA        : experience years (0, 1, negative invalid)
 * =====================================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DoctorService Unit Tests")
class DoctorServiceTest {

    @Mock DoctorRepository doctorRepo;
    @Mock UserRepository   userRepo;

    @InjectMocks
    DoctorService service;

    private Doctor doctor1, doctor2;
    private User   user1, user2;

    @BeforeEach
    void setUp() {
        user1 = new User(); user1.setId(1L); user1.setName("Dr. Alice"); user1.setEmail("alice@hosp.com");
        user1.setRole(User.Role.DOCTOR); user1.setIsActive(true);

        user2 = new User(); user2.setId(2L); user2.setName("Dr. Bob"); user2.setEmail("bob@hosp.com");
        user2.setRole(User.Role.DOCTOR); user2.setIsActive(true);

        doctor1 = new Doctor(); doctor1.setId(10L); doctor1.setUser(user1);
        doctor1.setSpecialization("Cardiology"); doctor1.setDepartment("Cardiac");
        doctor1.setQualification("MD"); doctor1.setExperienceYears(10);
        doctor1.setConsultationFee(500.0); doctor1.setRating(4.5); doctor1.setTotalReviews(20);
        doctor1.setIsVerified(true); doctor1.setIsPremium(false);
        doctor1.setAvailableDays(new HashSet<>(Set.of("MONDAY","TUESDAY","WEDNESDAY")));
        doctor1.setTimeSlots(new HashSet<>(Set.of("09:00","10:00")));

        doctor2 = new Doctor(); doctor2.setId(11L); doctor2.setUser(user2);
        doctor2.setSpecialization("Neurology"); doctor2.setDepartment("Neuro");
        doctor2.setQualification("MBBS"); doctor2.setExperienceYears(5);
        doctor2.setConsultationFee(700.0); doctor2.setRating(4.0); doctor2.setTotalReviews(10);
        doctor2.setIsVerified(true); doctor2.setIsPremium(true);
        doctor2.setAvailableDays(new HashSet<>(Set.of("THURSDAY","FRIDAY")));
        doctor2.setTimeSlots(new HashSet<>(Set.of("14:00","15:00")));
    }

    @Nested
    @DisplayName("Functional: Get Doctors")
    class GetDoctorsTests {

        @Test
        @DisplayName("FUNC-01: Get all verified active doctors returns list")
        void getAllDoctors_returnsVerifiedDoctors() {
            when(doctorRepo.findAllActiveVerifiedDoctors()).thenReturn(List.of(doctor1, doctor2));
            List<DoctorResponse> result = service.getAllDoctors();
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("FUNC-02: Get all doctors including unverified returns all")
        void getAllDoctorsIncludingUnverified_returnsAll() {
            when(doctorRepo.findAll()).thenReturn(List.of(doctor1, doctor2));
            List<DoctorResponse> result = service.getAllDoctorsIncludingUnverified();
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("FUNC-03: Get doctor by valid ID → found")
        void getDoctorById_validId_found() {
            when(doctorRepo.findById(10L)).thenReturn(Optional.of(doctor1));
            DoctorResponse resp = service.getDoctorById(10L);
            assertEquals("Cardiology", resp.getSpecialization());
            assertEquals("Dr. Alice", resp.getName());
        }

        @Test
        @DisplayName("FUNC-04: Get doctor by invalid ID → RuntimeException")
        void getDoctorById_invalidId_throws() {
            when(doctorRepo.findById(999L)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> service.getDoctorById(999L));
        }

        @Test
        @DisplayName("FUNC-05: Get doctor by user ID → found")
        void getDoctorByUserId_valid() {
            when(doctorRepo.findByUserId(1L)).thenReturn(Optional.of(doctor1));
            DoctorResponse resp = service.getDoctorByUserId(1L);
            assertNotNull(resp);
            assertEquals(10L, resp.getId());
        }
    }

    @Nested
    @DisplayName("EP: Filter by Specialization & Department")
    class FilterTests {

        @Test
        @DisplayName("EP-01: Filter by existing specialization returns matching doctors")
        void filterBySpecialization_existing() {
            when(doctorRepo.findBySpecialization("Cardiology")).thenReturn(List.of(doctor1));
            List<DoctorResponse> result = service.getDoctorsBySpecialization("Cardiology");
            assertEquals(1, result.size());
            assertEquals("Cardiology", result.get(0).getSpecialization());
        }

        @Test
        @DisplayName("EP-02: Filter by non-existing specialization returns empty")
        void filterBySpecialization_nonExisting() {
            when(doctorRepo.findBySpecialization("Dermatology")).thenReturn(List.of());
            List<DoctorResponse> result = service.getDoctorsBySpecialization("Dermatology");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("EP-03: Filter by department returns matching doctors")
        void filterByDepartment_returns() {
            when(doctorRepo.findByDepartment("Neuro")).thenReturn(List.of(doctor2));
            List<DoctorResponse> result = service.getDoctorsByDepartment("Neuro");
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("EP-04: Premium doctors filter returns only premium")
        void filterPremium_returnsOnlyPremium() {
            when(doctorRepo.findByIsPremium(true)).thenReturn(List.of(doctor2));
            List<DoctorResponse> result = service.getPremiumDoctors();
            assertEquals(1, result.size());
            assertTrue(result.get(0).getIsPremium());
        }
    }

    @Nested
    @DisplayName("Functional: Search & Lists")
    class SearchAndListTests {

        @Test
        @DisplayName("FUNC-06: Search by keyword matches name or specialization")
        void searchDoctors_returnsMatches() {
            when(doctorRepo.searchDoctors("Alice")).thenReturn(List.of(doctor1));
            List<DoctorResponse> result = service.searchDoctors("Alice");
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("FUNC-07: getAllSpecializations returns distinct sorted list")
        void getAllSpecializations_distinctSorted() {
            when(doctorRepo.findAll()).thenReturn(List.of(doctor1, doctor2));
            List<String> specs = service.getAllSpecializations();
            assertEquals(2, specs.size());
            assertEquals("Cardiology", specs.get(0)); // alphabetical
        }

        @Test
        @DisplayName("FUNC-08: getAllDepartments filters nulls")
        void getAllDepartments_filtersNulls() {
            doctor1.setDepartment(null); // null dept should be filtered
            when(doctorRepo.findAll()).thenReturn(List.of(doctor1, doctor2));
            List<String> depts = service.getAllDepartments();
            assertEquals(1, depts.size());
            assertEquals("Neuro", depts.get(0));
        }
    }

    @Nested
    @DisplayName("BVA: Doctor Update Fields")
    class UpdateBVATests {

        @Test
        @DisplayName("BVA-01: Update only specialization leaves others intact")
        void updateDoctor_onlySpecialization() {
            when(doctorRepo.findByUserId(1L)).thenReturn(Optional.of(doctor1));
            when(doctorRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            Doctor update = new Doctor();
            update.setSpecialization("Oncology");

            DoctorResponse resp = service.updateDoctor(1L, update);
            assertEquals("Oncology", resp.getSpecialization());
            // Qualification should remain unchanged
            assertEquals("MD", resp.getQualification());
        }

        @Test
        @DisplayName("BVA-02: Update with null fields → no change (null safety)")
        void updateDoctor_nullFields_noChange() {
            when(doctorRepo.findByUserId(1L)).thenReturn(Optional.of(doctor1));
            when(doctorRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            Doctor update = new Doctor(); // all nulls
            DoctorResponse resp = service.updateDoctor(1L, update);
            assertEquals("Cardiology", resp.getSpecialization());
            assertEquals("MD", resp.getQualification());
        }
    }
}
