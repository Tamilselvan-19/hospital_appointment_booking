# Requirements Traceability Matrix (RTM)
## Hospital Appointment Booking System

**Project:** Hospital Appointment Booking System  
**Version:** 1.0.0  
**Date:** 2026-06-16  

---

## 1. REQUIREMENTS → TEST CASE MAPPING

| Req ID | Requirement Description | Test Class | Test Method | Test Type | Status |
|--------|------------------------|------------|-------------|-----------|--------|
| REQ-001 | Patient must be able to register with name, email, password, phone | `AuthServiceTest` | `register_validPatient_success` | Unit | ✅ Covered |
| REQ-001 | Patient must be able to register with name, email, password, phone | `FullIntegrationTest` | `uat_registerPatient_success` | Integration/UAT | ✅ Covered |
| REQ-001 | Patient must be able to register with name, email, password, phone | `ApiContractTest` | `api_01_registerPatient` | API Contract | ✅ Covered |
| REQ-002 | System must reject duplicate email at registration | `AuthServiceTest` | `register_duplicateEmail_error` | Unit (EP) | ✅ Covered |
| REQ-002 | System must reject duplicate email at registration | `DefectLifecycleRegressionTest` | `bug006_duplicateEmail_userFriendlyError` | Regression | ✅ Covered |
| REQ-002 | System must reject duplicate email at registration | `FullIntegrationTest` | `reg_duplicateEmail_returnsError` | Integration | ✅ Covered |
| REQ-003 | System must reject duplicate phone at registration | `AuthServiceTest` | `register_duplicatePhone_error` | Unit (EP) | ✅ Covered |
| REQ-004 | User must be able to log in with email + password and receive JWT | `AuthServiceTest` | `login_validCredentials_returnsJwt` | Unit | ✅ Covered |
| REQ-004 | User must be able to log in with email + password and receive JWT | `ApiContractTest` | `api_03_loginPatient_captureToken` | API Contract | ✅ Covered |
| REQ-005 | System must reject login with wrong password | `AuthServiceTest` | `login_invalidCredentials_throws` | Unit | ✅ Covered |
| REQ-005 | System must reject login with wrong password | `ApiContractTest` | `api_14_wrongPasswordRejected` | API Contract | ✅ Covered |
| REQ-006 | Default registration role must be PATIENT | `AuthServiceTest` | `register_defaultRoleIsPatient` | Unit | ✅ Covered |
| REQ-007 | Password change must require old password match | `AuthServiceTest` | `changePassword_wrongOldPassword_rejected` | Unit (BVA) | ✅ Covered |
| REQ-008 | New password must be at least 6 characters | `AuthServiceTest` | `changePassword_5chars_rejected` | Unit (BVA-1) | ✅ Covered |
| REQ-008 | New password must be at least 6 characters | `AuthServiceTest` | `changePassword_6chars_accepted` | Unit (BVA) | ✅ Covered |
| REQ-008 | New password must be at least 6 characters | `DefectLifecycleRegressionTest` | `bug004_shortPasswordRejected` | Regression | ✅ Covered |
| REQ-009 | Patient must be able to book appointment with valid doctor, date, time | `AppointmentServiceTest` | `smokeBookAppointment_success` | Unit (Smoke) | ✅ Covered |
| REQ-009 | Patient must be able to book appointment with valid doctor, date, time | `ApiContractTest` | `api_08_bookAppointment` | API Contract | ✅ Covered |
| REQ-010 | Appointment date must not be in the past | `AppointmentServiceTest` | `bva_pastDate_yesterday_rejected` | Unit (BVA) | ✅ Covered |
| REQ-010 | Appointment date must not be in the past | `AppointmentServiceTest` | `bva_pastDate_today_rejected` | Unit (BVA) | ✅ Covered |
| REQ-010 | Appointment date must not be in the past | `DefectLifecycleRegressionTest` | `bug001_pastDateBooking_rejected` | Regression | ✅ Covered |
| REQ-011 | Future dates must be accepted for booking | `AppointmentServiceTest` | `bva_tomorrowDate_accepted_ifDoctorAvailable` | Unit (BVA+1) | ✅ Covered |
| REQ-011 | Future dates must be accepted for booking | `AppointmentServiceTest` | `bva_farFutureDate_accepted` | Unit (BVA) | ✅ Covered |
| REQ-012 | Booking must only use doctor's configured time slots | `AppointmentServiceTest` | `ep_validSlots_accepted` | Unit (EP) | ✅ Covered |
| REQ-012 | Booking must only use doctor's configured time slots | `AppointmentServiceTest` | `ep_invalidSlots_rejected` | Unit (EP) | ✅ Covered |
| REQ-013 | Already-booked slot must be rejected (no double booking) | `AppointmentServiceTest` | `ep_alreadyBookedSlot_rejected` | Unit (EP) | ✅ Covered |
| REQ-013 | Already-booked slot must be rejected (no double booking) | `AppointmentServiceTest` | `reg_slotAvailability_returnsFalse_whenBooked` | Unit (Regression) | ✅ Covered |
| REQ-014 | Doctor not working on weekend must reject weekend booking | `AppointmentServiceTest` | `ep_weekendBooking_rejected` | Unit (EP) | ✅ Covered |
| REQ-015 | Appointment status must follow: PENDING → CONFIRMED → COMPLETED | `AppointmentServiceTest` | `func_confirmPending_success` | Unit (Functional) | ✅ Covered |
| REQ-015 | Appointment status must follow: PENDING → CONFIRMED → COMPLETED | `AppointmentServiceTest` | `func_completeAppointment_success` | Unit (Functional) | ✅ Covered |
| REQ-016 | Only PENDING appointments can be confirmed | `AppointmentServiceTest` | `func_confirmNonPending_throws` | Unit (Functional) | ✅ Covered |
| REQ-017 | Patient OR Doctor can cancel an appointment | `AppointmentServiceTest` | `func_cancelByPatient_success` | Unit (Functional) | ✅ Covered |
| REQ-017 | Patient OR Doctor can cancel an appointment | `AppointmentServiceTest` | `func_cancelByDoctor_success` | Unit (Functional) | ✅ Covered |
| REQ-017 | Patient OR Doctor can cancel an appointment | `ApiContractTest` | `api_13_patientCancelsAppointment` | API Contract | ✅ Covered |
| REQ-018 | Unrelated user cannot cancel an appointment | `AppointmentServiceTest` | `func_cancelByStranger_throws` | Unit (Regression) | ✅ Covered |
| REQ-019 | Completed appointments cannot be cancelled | `AppointmentServiceTest` | `func_cancelCompleted_throws` | Unit (Functional) | ✅ Covered |
| REQ-020 | Patient can only view their own appointments (not others') | `AppointmentServiceTest` | `reg_ownershipCheck_deniedForStranger` | Unit (Regression) | ✅ Covered |
| REQ-020 | Patient can only view their own appointments (not others') | `AppointmentServiceTest` | `reg_ownershipCheck_allowedForOwner` | Unit (Regression) | ✅ Covered |
| REQ-021 | No-show status must be settable by doctor | `AppointmentServiceTest` | `func_markNoShow_success` | Unit (Functional) | ✅ Covered |
| REQ-021 | No-show status must be settable by doctor | `AppointmentControllerTest` | `markNoShow_doctor_200` | Integration | ✅ Covered |
| REQ-022 | Doctor list must be publicly accessible without auth | `ApiContractTest` | `api_05_getDoctors_public` | API Contract | ✅ Covered |
| REQ-022 | Doctor list must be publicly accessible without auth | `FullIntegrationTest` | `smokePublicDoctorsList_200` | Smoke | ✅ Covered |
| REQ-023 | Doctors can be filtered by specialization | `DoctorServiceTest` | `filterBySpecialization_existing` | Unit (EP) | ✅ Covered |
| REQ-024 | Search returns empty for unknown specialization | `DoctorServiceTest` | `filterBySpecialization_nonExisting` | Unit (EP) | ✅ Covered |
| REQ-025 | Only verified doctors appear in public list | `DoctorServiceTest` | `getAllDoctors_returnsVerifiedDoctors` | Unit (Functional) | ✅ Covered |
| REQ-026 | Available time slots must exclude already-booked slots | `AppointmentServiceTest` | `reg_slotAvailability_returnsTrue_whenFree` | Unit | ✅ Covered |
| REQ-027 | Unauthenticated users cannot access protected endpoints | `AppointmentControllerTest` | `secUnauthenticated_patientEndpoint` | Security | ✅ Covered |
| REQ-027 | Unauthenticated users cannot access protected endpoints | `DefectLifecycleRegressionTest` | `bug008_unauthAccessPatientEndpoint_blocked` | Security/Regression | ✅ Covered |
| REQ-028 | DOCTOR role cannot book appointments (patient-only) | `AppointmentControllerTest` | `secDoctorCannotBookAppointment` | Security | ✅ Covered |
| REQ-028 | DOCTOR role cannot book appointments (patient-only) | `DefectLifecycleRegressionTest` | `bug009_doctorCannotBookAppointments` | Security/Regression | ✅ Covered |
| REQ-029 | PATIENT role cannot confirm appointments (doctor-only) | `AppointmentControllerTest` | `secPatientCannotConfirmAppointment` | Security | ✅ Covered |
| REQ-029 | PATIENT role cannot confirm appointments (doctor-only) | `ApiContractTest` | `api_17_crossRoleAccess_forbidden` | API Contract | ✅ Covered |
| REQ-030 | Doctor profile must be updated without touching null fields | `DoctorServiceTest` | `updateDoctor_nullFields_noChange` | Unit (BVA) | ✅ Covered |
| REQ-031 | Registered doctor must appear in public listing | `FullIntegrationTest` | `uat_registeredDoctorAppearsInList` | UAT | ✅ Covered |

---

## 2. TEST COVERAGE SUMMARY

| Test Type | Test Classes | # Test Methods | Requirement IDs Covered |
|-----------|-------------|----------------|------------------------|
| Unit (Mockito + JUnit5) | AppointmentServiceTest, AuthServiceTest, DoctorServiceTest | 35 | REQ-001 to REQ-021, REQ-023-026, REQ-030 |
| Integration (@WebMvcTest) | AppointmentControllerTest | 8 | REQ-022, REQ-027-029 |
| Integration (@SpringBootTest) | FullIntegrationTest | 13 | REQ-001, REQ-004, REQ-022, REQ-031 |
| API Contract (Postman-style) | ApiContractTest | 17 | REQ-001-005, REQ-009, REQ-017, REQ-022, REQ-029 |
| Defect Regression | DefectLifecycleRegressionTest | 5 | REQ-002, REQ-008, REQ-010, REQ-027, REQ-028 |
| **TOTAL** | **5 classes** | **~78 tests** | **REQ-001 to REQ-031 (31 requirements)** |

---

## 3. DEFECT LOG

| Bug ID | Title | Severity | Component | Test | Status |
|--------|-------|----------|-----------|------|--------|
| BUG-001 | Past-date appointment could be booked | HIGH | AppointmentService | `bug001_pastDateBooking_rejected` | CLOSED |
| BUG-002 | Patient could view another patient's appointment | HIGH | AppointmentService | `reg_ownershipCheck_deniedForStranger` | CLOSED |
| BUG-003 | Double-booking not prevented | HIGH | AppointmentService | `ep_alreadyBookedSlot_rejected` | CLOSED |
| BUG-004 | Password < 6 chars was accepted | MEDIUM | AuthService | `bug004_shortPasswordRejected` | CLOSED |
| BUG-005 | Currency field blank in payment order | LOW | PaymentService | Covered by code review | CLOSED |
| BUG-006 | Duplicate email returned HTTP 500 | MEDIUM | AuthService | `bug006_duplicateEmail_userFriendlyError` | CLOSED |
| BUG-007 | Completed appointment could be cancelled | HIGH | AppointmentService | `func_cancelCompleted_throws` | CLOSED |
| BUG-008 | Protected endpoints accessible without JWT | HIGH | SecurityConfig | `bug008_unauthAccessPatientEndpoint_blocked` | CLOSED |
| BUG-009 | Doctor could book patient appointments | HIGH | SecurityConfig | `bug009_doctorCannotBookAppointments` | CLOSED |

---

## 4. TESTING TECHNIQUES MATRIX

| Technique | Applied In | Description |
|-----------|-----------|-------------|
| **Boundary Value Analysis (BVA)** | `AppointmentServiceTest`, `AuthServiceTest` | Tests at exact boundary (today/yesterday/tomorrow, 5/6/7 chars password) |
| **Equivalence Partitioning (EP)** | `AppointmentServiceTest`, `DoctorServiceTest` | Tests valid/invalid partitions for slots, days, specializations |
| **Smoke Testing** | `FullIntegrationTest`, `AppointmentControllerTest` | Quick pass/fail on critical paths before deeper tests |
| **Functional Testing** | `AppointmentServiceTest`, `DoctorServiceTest` | Tests individual features match specification |
| **Regression Testing** | `DefectLifecycleRegressionTest`, `AppointmentServiceTest` | Re-runs fixed bugs to ensure they don't re-appear |
| **UAT (User Acceptance)** | `FullIntegrationTest` | End-to-end user journeys: register→login→book→view |
| **API Contract Testing (Postman-style)** | `ApiContractTest` | Sequential HTTP calls mirroring a Postman Collection Runner |
| **Security Testing** | `AppointmentControllerTest`, `DefectLifecycleRegressionTest` | Role-based access control, JWT validation |
| **Mockito (Mocking)** | `AppointmentServiceTest`, `AuthServiceTest`, `DoctorServiceTest` | Isolates service under test from DB/infrastructure |
| **Page Object Model concept** | `AppointmentControllerTest` | MockMvc setup as reusable "page actions" in `@BeforeEach` |
| **CI/CD Integration** | `.github/workflows/ci-cd.yml` | 4-stage pipeline: compile → unit → integration → artifact |
