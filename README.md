# 🏥 Hospital Appointment Booking System

A full-stack hospital management platform built with **Spring Boot 3.2**, **MySQL**, **JWT authentication**, and **Thymeleaf** templates. Patients can register, search doctors, and book appointments online. Doctors can manage their schedules and confirm appointments. Payments are handled through the Cashfree payment gateway.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup & Installation](#setup--installation)
- [Running the Application](#running-the-application)
- [API Overview](#api-overview)
- [Testing](#testing)
- [CI/CD Pipeline](#cicd-pipeline)
- [Known Bugs Fixed](#known-bugs-fixed)

---

## Features

- **Patient portal** — register, log in, browse verified doctors, book/cancel appointments, pay online
- **Doctor portal** — manage profile, configure schedule and time slots, confirm/complete appointments
- **JWT-based authentication** — stateless REST API with role-based access control (PATIENT / DOCTOR / ADMIN)
- **Cashfree payment integration** — sandbox-ready payment order creation and verification
- **Global exception handling** — user-friendly error responses for all failure scenarios
- **H2 in-memory database for tests** — no MySQL required to run the test suite

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security 6 + JWT (JJWT 0.12.3) |
| Persistence | Spring Data JPA / Hibernate + MySQL 8 |
| Templates | Thymeleaf + Thymeleaf Security Extras |
| Build | Maven |
| Testing | JUnit 5, Mockito, MockMvc, H2 |
| CI/CD | GitHub Actions |
| Payments | Cashfree Payment Gateway (sandbox) |

---

## Project Structure

```
hospital-booking-system/
├── src/
│   ├── main/
│   │   ├── java/com/hospital/booking/
│   │   │   ├── config/          # SecurityConfig, DataInitializer
│   │   │   ├── controller/      # REST controllers (Auth, Appointment, Doctor, Patient, Payment)
│   │   │   ├── dto/             # Request/Response DTOs
│   │   │   ├── entity/          # JPA entities (User, Doctor, Patient, Appointment, Payment)
│   │   │   ├── exception/       # GlobalExceptionHandler
│   │   │   ├── repository/      # Spring Data JPA repositories
│   │   │   ├── security/        # JwtUtils, JwtAuthenticationFilter, UserDetailsImpl
│   │   │   └── service/         # Business logic (Auth, Appointment, Doctor, Patient, Payment)
│   │   └── resources/
│   │       ├── templates/       # Thymeleaf HTML pages (patient/, doctor/)
│   │       ├── static/js/       # auth-guard.js
│   │       ├── application.properties
│   │       └── schema.sql
│   └── test/
│       └── java/com/hospital/booking/
│           ├── service/         # Unit tests (Mockito)
│           ├── controller/      # Web layer tests (@WebMvcTest)
│           └── integration/     # Full integration & regression tests
├── .github/workflows/ci-cd.yml
├── pom.xml
├── RTM.md                       # Requirements Traceability Matrix
└── MANUAL_TEST_CASES.md
```

---

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+ (for running the app; tests use H2 — no MySQL needed)
- Git

---

## Setup & Installation

### 1. Clone the repository

```bash
git clone https://github.com/<your-org>/hospital-booking-system.git
cd hospital-booking-system
```

### 2. Create the MySQL database

```sql
CREATE DATABASE hospital_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Configure database credentials

Open `src/main/resources/application.properties` and update:

```properties
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
```

Or set environment variables instead:

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
```

### 4. (Optional) Configure Cashfree

If you want payment flows to work, set your Cashfree sandbox credentials:

```bash
export CASHFREE_APP_ID=your_app_id
export CASHFREE_SECRET_KEY=your_secret_key
```

### 5. (Optional) Override JWT secret

A default Base64 JWT secret is set in `application.properties`. Override it for production:

```bash
export JWT_SECRET=your_base64_encoded_secret_at_least_32_chars
```

---

## Running the Application

```bash
mvn spring-boot:run
```

The app starts on **http://localhost:8080**.

Default seed data (created by `DataInitializer` on first boot):
- Admin user: `admin@hospital.com` / `Admin@123`
- Sample doctors with pre-configured schedules

---

## API Overview

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register patient or doctor |
| POST | `/api/auth/login` | Public | Login and receive JWT |
| POST | `/api/auth/refresh` | Public | Refresh access token |

### Doctors

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/doctors` | Public | List all verified doctors |
| GET | `/api/doctors/{id}` | Public | Get doctor details |
| GET | `/api/doctors/{id}/slots?date=YYYY-MM-DD` | Public | Get available time slots |

### Appointments (Patient)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/appointments` | PATIENT | Book an appointment |
| GET | `/api/patient/appointments` | PATIENT | List upcoming appointments |
| GET | `/api/patient/appointments/all` | PATIENT | List all appointments |
| DELETE | `/api/appointments/{id}` | PATIENT | Cancel an appointment |

### Appointments (Doctor)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/doctor/appointments` | DOCTOR | View assigned appointments |
| PUT | `/api/doctor/appointments/{id}/confirm` | DOCTOR | Confirm a pending appointment |
| PUT | `/api/doctor/appointments/{id}/complete` | DOCTOR | Mark appointment as completed |
| PUT | `/api/doctor/appointments/{id}/no-show` | DOCTOR | Mark patient as no-show |

### Payments

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/payments/create-order` | PATIENT | Create Cashfree payment order |
| POST | `/api/payments/verify` | PATIENT | Verify payment after callback |

---

## Testing

The test suite covers **~78 test methods** across **5 test classes**, verifying **31 requirements** (see `RTM.md` for full traceability). Tests are split into two layers with distinct purposes.

### Test layers at a glance

| Layer | Class(es) | Technique | What it checks | DB |
|---|---|---|---|---|
| Unit | `AppointmentServiceTest`, `AuthServiceTest`, `DoctorServiceTest`, `PaymentServiceTest` | Mockito + JUnit 5 | Business rules in isolation, no Spring context | None (mocked) |
| Web layer | `AppointmentControllerTest` | `@WebMvcTest` + MockMvc | HTTP routing, request validation, role security | None (service mocked) |
| Integration | `FullIntegrationTest`, `ApiContractTest` | `@SpringBootTest` + MockMvc | Full JWT flows, end-to-end user journeys | H2 in-memory |
| Regression | `DefectLifecycleRegressionTest` | `@SpringBootTest` + MockMvc | Proves fixed bugs cannot resurface | H2 in-memory |

---

### Unit Tests — `src/test/java/.../service/`

**Where:** Service layer only. No Spring context is loaded. No database. All dependencies are replaced with Mockito mocks.

**Why:** Unit tests are fast (milliseconds each) and pinpoint exactly which business rule broke when a test fails. They run on every commit in CI.

**Techniques used:**

- **Boundary Value Analysis (BVA)** — tests values at the exact edge of valid ranges. For example, `bva_pastDate_yesterday_rejected` passes `LocalDate.now().minusDays(1)` and expects rejection; `bva_tomorrowDate_accepted_ifDoctorAvailable` passes `now().plusDays(1)` and expects success. Password length is tested at 5 chars (reject), 6 chars (accept), and 7 chars (accept).
- **Equivalence Partitioning (EP)** — tests one representative from each valid/invalid group. For example, `ep_validSlots_accepted` sends a time slot from the doctor's configured list; `ep_invalidSlots_rejected` sends one that is not configured. This avoids testing every possible slot while still covering all logical partitions.
- **Smoke** — `smokeBookAppointment_success` verifies the happy path end-to-end through the service layer with valid inputs.
- **Regression** — individual tests named after bug IDs (e.g., `reg_ownershipCheck_deniedForStranger`) verify that previously fixed defects cannot reappear.

**How to run:**

```bash
mvn test -Dspring.profiles.active=test \
  -Dtest="AppointmentServiceTest,AuthServiceTest,DoctorServiceTest,PaymentServiceTest"
```

---

### Web Layer Tests — `src/test/java/.../controller/AppointmentControllerTest.java`

**Where:** Controller (HTTP) layer only. Uses `@WebMvcTest`, which loads only the web slice — controllers, security filters, and Jackson — without touching the database or service implementations.

**Why:** This layer validates that the HTTP contract is correct: routes map to the right controllers, role guards return the right HTTP status codes, and request bodies are deserialized properly. Because `AppointmentService` is a `@MockBean` here, any service-level failure is deliberately excluded — the test isolates the web layer exclusively.

**Techniques used:**

- **Security testing** — `secUnauthenticated_patientEndpoint` asserts that hitting a patient endpoint without a JWT token returns 401. `secDoctorCannotBookAppointment` asserts that a DOCTOR-role token on a patient-only endpoint returns 403.
- **EP** — valid vs invalid request bodies tested through `MockMvc.perform(post(...).content(...))` calls.
- **Smoke** — key endpoints return HTTP 200 for a correctly authenticated request with a valid body.

**How to run:**

```bash
mvn test -Dspring.profiles.active=test -Dtest="AppointmentControllerTest"
```

---

### Integration Tests — `src/test/java/.../integration/FullIntegrationTest.java`

**Where:** Full Spring Boot application context (`@SpringBootTest`) with an **H2 in-memory database** replacing MySQL. The test profile (`application-test.properties`) activates automatically.

**Why:** Integration tests verify that all layers — security filters, controllers, services, repositories, and the H2 schema — work together correctly. They simulate real HTTP calls through MockMvc against a live (in-memory) application, catching wiring bugs that unit tests cannot see.

**Techniques used:**

- **UAT (User Acceptance Testing) style** — tests follow complete user journeys: register a patient → login → receive JWT → use JWT to book an appointment → verify booking appears in appointment list.
- **Smoke** — `smokePublicDoctorsList_200` confirms the server starts and returns HTTP 200 on a public endpoint before running heavier tests.
- **Regression** — `reg_duplicateEmail_returnsError` ensures that registering with an existing email returns a 4xx error (not HTTP 500 as it did before BUG-006 was fixed).
- **Ordered execution** — `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` ensures tests run in sequence so the JWT token captured in step 2 (login) is available for step 3 (book appointment).

**How to run:**

```bash
mvn test -Dspring.profiles.active=test -Dtest="FullIntegrationTest"
```

---

### API Contract Tests — `src/test/java/.../integration/ApiContractTest.java`

**Where:** Same setup as `FullIntegrationTest` — full Spring Boot context with H2.

**Why:** These tests replace what you would otherwise write in a Postman collection. Each test corresponds to one API call with explicit assertions on HTTP status code, response body fields, and JSON structure. They document and enforce the API contract so that future refactoring cannot silently break clients.

**Endpoints exercised:** `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`, `GET /api/doctors`, `GET /api/doctors/{id}`, `GET /api/doctors/{id}/slots`, `POST /api/appointments`, `GET /api/patient/appointments`, `DELETE /api/appointments/{id}`, `GET /api/doctor/appointments`, `PUT /api/doctor/appointments/{id}/confirm`, cross-role access (forbidden).

**How to run:**

```bash
mvn test -Dspring.profiles.active=test -Dtest="ApiContractTest"
```

---

### Defect Lifecycle / Regression Tests — `src/test/java/.../integration/DefectLifecycleRegressionTest.java`

**Where:** Full Spring Boot context with H2, same as the integration tests.

**Why:** Every formally documented bug gets its own regression test. Once a bug is fixed and the test is added, the test acts as a permanent guard — if the same bug is accidentally reintroduced, the regression test catches it before it reaches production. Each test is named after the bug ID it covers.

**Bugs covered:**

| Bug ID | What the test verifies |
|---|---|
| BUG-001 | Booking with a past date is rejected with an appropriate error |
| BUG-004 | Password shorter than 6 characters is rejected during `changePassword` |
| BUG-006 | Registering a duplicate email returns a 4xx error, not HTTP 500 |
| BUG-008 | Unauthenticated requests to patient endpoints are blocked (401) |
| BUG-009 | A DOCTOR-role JWT cannot book appointments (patient-only operation) |

**How to run:**

```bash
mvn test -Dspring.profiles.active=test -Dtest="DefectLifecycleRegressionTest"
```

---

### Run all tests at once

```bash
mvn test -Dspring.profiles.active=test
```

### Run integration tests (Failsafe plugin)

```bash
mvn failsafe:integration-test failsafe:verify -Dspring.profiles.active=test
```

> **Note:** All tests use H2 in-memory database. You do not need MySQL running to execute the test suite.

---

## CI/CD Pipeline

The GitHub Actions workflow (`.github/workflows/ci-cd.yml`) runs automatically on every push and on pull requests to `main` or `develop`.

**Stage 1 — Smoke Gate** (every push): compiles the project. Fails fast in under 60 seconds if there are compilation errors.

**Stage 2 — Unit Tests** (every push, after Stage 1): runs `AppointmentServiceTest`, `AuthServiceTest`, and `DoctorServiceTest` with `spring.profiles.active=test`. No MySQL required.

**Stage 3 — Integration & API Tests** (every push, after Stage 2): runs the full test suite including `FullIntegrationTest`, `ApiContractTest`, `DefectLifecycleRegressionTest`, and `AppointmentControllerTest` against H2. Also invokes Maven Failsafe for integration-test phase verification.

**Stage 4 — Build JAR** (`main` branch only, after Stage 3): packages the application JAR and uploads it as a GitHub Actions artifact (retained 30 days).

Test results are uploaded as artifacts after each stage (`surefire-reports/`, `failsafe-reports/`) so failures can be inspected without re-running the build.

---

## Known Bugs Fixed

| Bug ID | Severity | Description | Fix Location |
|---|---|---|---|
| BUG-001 | HIGH | Past-date appointments could be booked | `AppointmentService.bookAppointment()` |
| BUG-002 | HIGH | Patient could view another patient's appointment | `AppointmentService.getAppointmentByIdForPatient()` |
| BUG-003 | HIGH | Double-booking the same slot was not prevented | `AppointmentService.isSlotAvailable()` |
| BUG-004 | MEDIUM | Passwords shorter than 6 characters were accepted during password change | `AuthService.changePassword()` |
| BUG-005 | LOW | Currency field was blank in payment orders | `PaymentRequest.getCurrency()` default value |
| BUG-006 | MEDIUM | Duplicate email registration returned HTTP 500 instead of a user-friendly error | `GlobalExceptionHandler` + `AuthService.registerUser()` |
| BUG-007 | HIGH | Completed appointments could be cancelled | `AppointmentService.cancelAppointment()` status guard |
| BUG-008 | HIGH | Protected endpoints were accessible without a JWT token | `SecurityConfig` filter chain |
| BUG-009 | HIGH | DOCTOR role could call patient-only booking endpoints | `SecurityConfig` role-based route guards |

All bugs have corresponding regression tests in `DefectLifecycleRegressionTest` or the service unit tests to prevent recurrence.

---

## License

Internal project — all rights reserved.
