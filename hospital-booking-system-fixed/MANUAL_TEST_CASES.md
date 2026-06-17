# Manual Test Case Design & Execution Document
## Hospital Appointment Booking System

**Author:** QA Engineer  
**Version:** 1.0  
**Date:** 2026-06-16  
**Testing Scope:** Functional, Regression, Smoke, UAT, BVA, EP  

---

## MODULE 1: USER AUTHENTICATION

### TC-AUTH-001: Patient Registration — Valid Data
| Field | Value |
|-------|-------|
| **Test Case ID** | TC-AUTH-001 |
| **Test Type** | Functional / Smoke |
| **Priority** | P1 — Critical |
| **Preconditions** | Application running; email not already registered |
| **Test Data** | name="John Doe", email="john@test.com", phone="9876543210", password="Pass@123", role=PATIENT |
| **Steps** | 1. POST /api/auth/register with above JSON |
| **Expected Result** | HTTP 200, `{"success": true, "message": "Registration successful!..."}` |
| **Actual Result** | _(Fill during execution)_ |
| **Pass/Fail** | _(Fill during execution)_ |
| **Automated?** | YES — `AuthServiceTest.register_validPatient_success` + `FullIntegrationTest.uat_registerPatient_success` |

---

### TC-AUTH-002: Registration — Duplicate Email (EP: Invalid Partition)
| Field | Value |
|-------|-------|
| **Test Case ID** | TC-AUTH-002 |
| **Test Type** | EP — Invalid Partition, Regression (BUG-006) |
| **Priority** | P1 |
| **Preconditions** | User with john@test.com already exists |
| **Test Data** | email="john@test.com" (duplicate) |
| **Steps** | 1. POST /api/auth/register with duplicate email |
| **Expected Result** | HTTP 200, `{"success": false, "message": "Email is already registered"}` — NOT HTTP 500 |
| **Actual Result** | _(Fill during execution)_ |
| **Pass/Fail** | _(Fill during execution)_ |
| **Automated?** | YES — `AuthServiceTest.register_duplicateEmail_error` |

---

### TC-AUTH-003 to TC-AUTH-009: Password Change — BVA Table

| TC ID | Password Length | Input | Expected | BVA Point |
|-------|----------------|-------|----------|-----------|
| TC-AUTH-003 | 0 (empty) | `""` | REJECTED — "6 characters" | Below min - invalid |
| TC-AUTH-004 | 1 char | `"a"` | REJECTED | Well below |
| TC-AUTH-005 | 5 chars (BVA-1) | `"12345"` | REJECTED — "6 characters" | **Boundary - 1** |
| TC-AUTH-006 | 6 chars (BVA) | `"123456"` | ACCEPTED | **Exact Boundary** |
| TC-AUTH-007 | 7 chars (BVA+1) | `"1234567"` | ACCEPTED | **Boundary + 1** |
| TC-AUTH-008 | 50 chars | `"a"×50` | ACCEPTED | Middle valid |
| TC-AUTH-009 | 100 chars | `"a"×100` | ACCEPTED | Upper valid |

**Automated:** YES — `AuthServiceTest` BVA nested class (7 tests)

---

### TC-AUTH-010: Login — Wrong Password (EP: Invalid Partition)
| Field | Value |
|-------|-------|
| **Test Case ID** | TC-AUTH-010 |
| **Test Type** | EP — Invalid, Security |
| **Steps** | POST /api/auth/login with correct email + wrong password |
| **Expected Result** | HTTP 401 Unauthorized or error response |
| **Automated?** | YES — `AuthServiceTest.login_invalidCredentials_throws` |

---

## MODULE 2: APPOINTMENT BOOKING

### TC-APT-001 to TC-APT-005: Appointment Date — BVA Table

| TC ID | Date Input | Expected | BVA Point |
|-------|-----------|----------|-----------|
| TC-APT-001 | 30 days ago | REJECTED — "past" | Well below boundary |
| TC-APT-002 | Yesterday (BVA-1) | REJECTED — "past" | **Boundary - 1** |
| TC-APT-003 | Today (BVA) | REJECTED — "past" | **Exact Boundary** |
| TC-APT-004 | Tomorrow (BVA+1) | ACCEPTED (if doctor available) | **Boundary + 1** |
| TC-APT-005 | 1 year future | ACCEPTED | Upper valid |

**Automated:** YES — `AppointmentServiceTest` BVA nested class

---

### TC-APT-006: Book Appointment — Valid Data (Smoke)
| Field | Value |
|-------|-------|
| **Test Case ID** | TC-APT-006 |
| **Test Type** | Smoke, Functional |
| **Priority** | P1 |
| **Preconditions** | Patient logged in; doctor with available slots on MONDAY exists |
| **Test Data** | doctorId=1, date=next Monday, time="09:00", type=REGULAR |
| **Steps** | 1. GET /api/doctors (find doctor); 2. GET /api/appointments/{id}/slots?date=...; 3. POST /api/appointments |
| **Expected Result** | HTTP 200, status=PENDING, paid=false, fee=doctor's fee |
| **Automated?** | YES — `ApiContractTest` flow API-05 → API-08 |

---

### TC-APT-007 to TC-APT-012: Time Slot — EP Table

| TC ID | Slot | Doctor Slots | Expected | EP Partition |
|-------|------|-------------|----------|--------------|
| TC-APT-007 | "09:00" | ✅ configured | ACCEPTED | **Valid** |
| TC-APT-008 | "10:00" | ✅ configured | ACCEPTED | Valid |
| TC-APT-009 | "08:00" | ❌ not configured | REJECTED | **Invalid** |
| TC-APT-010 | "12:00" | ❌ not configured | REJECTED | Invalid (lunch) |
| TC-APT-011 | "09:00" (booked) | ✅ but taken | REJECTED | **Already booked** |
| TC-APT-012 | Saturday | N/A | REJECTED | **Invalid day** |

**Automated:** YES — `AppointmentServiceTest` EP nested class

---

### TC-APT-013: Status Flow — PENDING → CONFIRMED → COMPLETED
| Step | Action | Actor | Expected Status |
|------|--------|-------|----------------|
| 1 | Book appointment | Patient | PENDING |
| 2 | Doctor confirms | Doctor | CONFIRMED |
| 3 | Doctor completes with diagnosis | Doctor | COMPLETED |
| 4 | Patient tries to cancel | Patient | ERROR — "cannot be cancelled" |

**Test Case ID:** TC-APT-013  
**Automated:** YES — multiple `AppointmentServiceTest` functional tests + `ApiContractTest` API-12

---

### TC-APT-014: Cancel by Unauthorized User (Regression: BUG-002 style)
| Field | Value |
|-------|-------|
| **Test Case ID** | TC-APT-014 |
| **Test Type** | Security, Regression |
| **Steps** | 1. Book appointment as Patient A; 2. Login as Patient B; 3. Attempt DELETE /api/appointments/{A's appointment id} |
| **Expected Result** | ERROR — "Not authorized to cancel this appointment" |
| **Automated?** | YES — `AppointmentServiceTest.func_cancelByStranger_throws` |

---

## MODULE 3: DOCTOR MANAGEMENT

### TC-DOC-001: Public Doctor Listing (Smoke)
| Field | Value |
|-------|-------|
| **Test Case ID** | TC-DOC-001 |
| **Test Type** | Smoke |
| **Steps** | GET /api/doctors (no auth header) |
| **Expected Result** | HTTP 200, array of doctors |
| **Automated?** | YES — `FullIntegrationTest.smokePublicDoctorsList_200` |

### TC-DOC-002 to TC-DOC-004: Specialization Filter — EP Table

| TC ID | Filter | Expected | EP Partition |
|-------|--------|----------|--------------|
| TC-DOC-002 | "Cardiology" (exists) | Non-empty list | **Valid** |
| TC-DOC-003 | "Dermatology" (none) | Empty list | **Valid but no match** |
| TC-DOC-004 | "" (empty) | Depends on impl | **Edge** |

**Automated:** YES — `DoctorServiceTest` EP nested class

---

## MODULE 4: SECURITY

### TC-SEC-001: Access Protected Endpoint Without JWT
| Field | Value |
|-------|-------|
| **Test Case ID** | TC-SEC-001 |
| **Test Type** | Security |
| **Priority** | P1 |
| **Steps** | GET /api/patient/appointments — no Authorization header |
| **Expected Result** | HTTP 401 or 302 redirect to login |
| **Automated?** | YES — `AppointmentControllerTest.secUnauthenticated_patientEndpoint` |

### TC-SEC-002: Cross-Role Access (Doctor tries Patient endpoint)
| Field | Value |
|-------|-------|
| **Test Case ID** | TC-SEC-002 |
| **Steps** | POST /api/appointments with DOCTOR JWT token |
| **Expected Result** | HTTP 403 Forbidden |
| **Automated?** | YES — `AppointmentControllerTest.secDoctorCannotBookAppointment` |

### TC-SEC-003: Invalid JWT Token
| Field | Value |
|-------|-------|
| **Test Case ID** | TC-SEC-003 |
| **Steps** | GET /api/patient/appointments with Authorization: Bearer INVALID.TOKEN |
| **Expected Result** | HTTP 401 |
| **Automated?** | YES — `FullIntegrationTest.reg_invalidJwt_401` |

---

## MODULE 5: UAT SCENARIOS (End-to-End)

### UAT-001: Complete Patient Journey
```
1. Register as patient (TC-AUTH-001) ✅
2. Login → receive JWT (TC-AUTH-010 inverse) ✅
3. Browse doctors (TC-DOC-001) ✅
4. View available slots for a doctor ✅
5. Book appointment (TC-APT-006) ✅
6. View my appointments ✅
7. Cancel appointment ✅
```
**Automated:** YES — `FullIntegrationTest` + `ApiContractTest` (ordered flow)

### UAT-002: Complete Doctor Journey
```
1. Register as doctor ✅
2. Login → receive JWT ✅
3. View pending appointments ✅
4. Confirm an appointment ✅
5. Mark appointment complete with diagnosis ✅
6. View schedule ✅
```
**Automated:** YES — `ApiContractTest` API-04, 11, 12 + `AppointmentServiceTest` functional tests

---

## EXECUTION SUMMARY TEMPLATE

| Module | Total TCs | Pass | Fail | Blocked | Not Run |
|--------|-----------|------|------|---------|---------|
| Auth | 12 | | | | |
| Appointment | 14 | | | | |
| Doctor | 6 | | | | |
| Security | 5 | | | | |
| UAT | 2 | | | | |
| **TOTAL** | **39** | | | | |

**Entry Criteria:** Application compiles, H2 DB available for tests  
**Exit Criteria:** 100% P1 tests pass, no open CRITICAL/HIGH defects  
**Sign-off:** QA Lead _____________ Date _____________
