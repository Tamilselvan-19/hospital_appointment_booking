# 🏥 Hospital Appointment Booking System

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?style=flat-square&logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql)
![JWT](https://img.shields.io/badge/Auth-JWT-black?style=flat-square&logo=jsonwebtokens)
![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?style=flat-square&logo=githubactions)
![License](https://img.shields.io/badge/License-Internal-red?style=flat-square)

A production-grade, full-stack hospital management platform built with **Spring Boot 3.2**, **MySQL**, **JWT authentication**, and **Thymeleaf** templates. Patients can register, search doctors, and book appointments online. Doctors can manage their schedules and confirm appointments. Payments are handled through the **Cashfree** payment gateway.

---

## 📌 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture Overview](#-architecture-overview)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Setup & Installation](#-setup--installation)
- [Running the Application](#-running-the-application)
- [API Overview](#-api-overview)
- [Testing](#-testing)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Known Bugs Fixed](#-known-bugs-fixed)

---

## ✨ Features

| Portal | Capabilities |
|---|---|
| 🧑‍⚕️ **Patient** | Register, log in, browse verified doctors, book/cancel appointments, pay online |
| 👨‍⚕️ **Doctor** | Manage profile, configure schedule and time slots, confirm/complete appointments |
| 🔐 **Security** | JWT-based stateless auth with role-based access control (PATIENT / DOCTOR / ADMIN) |
| 💳 **Payments** | Cashfree sandbox-ready payment order creation and verification |
| 🛡️ **Reliability** | Global exception handling with user-friendly error responses for all failure scenarios |
| 🧪 **Testability** | H2-based test suite — no MySQL required to run the full test suite |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security 6 + JWT (JJWT 0.12.3) |
| Persistence | Spring Data JPA / Hibernate + MySQL 8 |
| Templates | Thymeleaf + Thymeleaf Security Extras |
| Build | Maven 3.8+ |
| Testing | JUnit 5, Mockito, MockMvc, H2 |
| CI/CD | GitHub Actions |
| Payments | Cashfree Payment Gateway (sandbox) |

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                        Client                           │
│          (Browser / REST Client / Thymeleaf UI)         │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTP
┌───────────────────────▼─────────────────────────────────┐
│              Spring Security Filter Chain               │
│         JWT Authentication Filter → Role Guards         │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│                  Controller Layer                        │
│   AuthController │ AppointmentController                │
│   DoctorController │ PatientController                  │
│   PaymentController                                     │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│                   Service Layer                          │
│   AuthService │ AppointmentService │ DoctorService       │
│   PatientService │ PaymentService                        │
└──────────┬────────────────────────────┬─────────────────┘
           │                            │
┌──────────▼──────────┐    ┌────────────▼────────────────┐
│  Repository Layer   │    │    External Integrations     │
│  Spring Data JPA    │    │  Cashfree Payment Gateway    │
└──────────┬──────────┘    └─────────────────────────────┘
           │
┌──────────▼──────────┐
│      Database       │
│   MySQL 8  (prod)   │
│   H2 in-memory      │
│       (tests)       │
└─────────────────────┘
```

---

## 📁 Project Structure

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
├── .github/
│   └── workflows/
│       └── ci-cd.yml
├── pom.xml
├── RTM.md                       # Requirements Traceability Matrix
└── MANUAL_TEST_CASES.md
```

---

## ✅ Prerequisites

| Tool | Version |
|---|---|
| Java | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ *(app only — tests use H2)* |
| Git | Any recent version |

---

## ⚙️ Setup & Installation

### 1. Clone the repository

```bash
git clone https://github.com/<your-org>/hospital-booking-system.git
cd hospital-booking-system
```

### 2. Create the MySQL database

```sql
CREATE DATABASE hospital_booking
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 3. Configure database credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
```

Or export as environment variables:

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
```

### 4. (Optional) Configure Cashfree

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

## 🚀 Running the Application

```bash
mvn spring-boot:run
```

App starts at **http://localhost:8080**

> **Default seed data** created by `DataInitializer` on first boot:
> - Admin user: `admin@hospital.com` / `Admin@123`
> - Sample doctors with pre-configured schedules

---

## 📡 API Overview

### 🔑 Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register as patient or doctor |
| `POST` | `/api/auth/login` | Public | Login and receive JWT |
| `POST` | `/api/auth/refresh` | Public | Refresh access token |

### 👨‍⚕️ Doctors

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/doctors` | Public | List all verified doctors |
| `GET` | `/api/doctors/{id}` | Public | Get doctor details |
| `GET` | `/api/doctors/{id}/slots?date=YYYY-MM-DD` | Public | Get available time slots |

### 🗓️ Appointments — Patient

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/appointments` | PATIENT | Book an appointment |
| `GET` | `/api/patient/appointments` | PATIENT | List upcoming appointments |
| `GET` | `/api/patient/appointments/all` | PATIENT | List all appointments |
| `DELETE` | `/api/appointments/{id}` | PATIENT | Cancel an appointment |

### 🗓️ Appointments — Doctor

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/doctor/appointments` | DOCTOR | View assigned appointments |
| `PUT` | `/api/doctor/appointments/{id}/confirm` | DOCTOR | Confirm a pending appointment |
| `PUT` | `/api/doctor/appointments/{id}/complete` | DOCTOR | Mark appointment as completed |
| `PUT` | `/api/doctor/appointments/{id}/no-show` | DOCTOR | Mark patient as no-show |

### 💳 Payments

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/payments/create-order` | PATIENT | Create Cashfree payment order |
| `POST` | `/api/payments/verify` | PATIENT | Verify payment after callback |

---

## 🧪 Testing

The suite covers **~78 test methods** across **5 test classes**, verifying **31 requirements** (see `RTM.md` for full traceability). All tests run against H2 in-memory — **no MySQL required**.

### Test Layers

| Layer | Class(es) | Technique | What it covers | DB |
|---|---|---|---|---|
| Unit | `AppointmentServiceTest`, `AuthServiceTest`, `DoctorServiceTest`, `PaymentServiceTest` | Mockito + JUnit 5 | Business rules in isolation | None (mocked) |
| Web | `AppointmentControllerTest` | `@WebMvcTest` + MockMvc | HTTP routing, role guards, request validation | None (mocked) |
| Integration | `FullIntegrationTest`, `ApiContractTest` | `@SpringBootTest` + MockMvc | Full JWT flows, end-to-end user journeys | H2 |
| Regression | `DefectLifecycleRegressionTest` | `@SpringBootTest` + MockMvc | Guards all fixed bugs from resurfacing | H2 |

### Running Tests

```bash
# Run the full test suite
mvn test -Dspring.profiles.active=test

# Unit tests only
mvn test -Dspring.profiles.active=test \
  -Dtest="AppointmentServiceTest,AuthServiceTest,DoctorServiceTest,PaymentServiceTest"

# Web layer tests only
mvn test -Dspring.profiles.active=test -Dtest="AppointmentControllerTest"

# Integration tests only
mvn test -Dspring.profiles.active=test -Dtest="FullIntegrationTest"

# API contract tests only
mvn test -Dspring.profiles.active=test -Dtest="ApiContractTest"

# Regression tests only
mvn test -Dspring.profiles.active=test -Dtest="DefectLifecycleRegressionTest"

# Integration phase via Failsafe plugin
mvn failsafe:integration-test failsafe:verify -Dspring.profiles.active=test
```

---

## 🔄 CI/CD Pipeline

Triggered automatically on every push and pull request to `main` or `develop`.

```
Push / PR to main or develop
        │
        ▼
┌───────────────────────────────────────────┐
│  Stage 1 — Smoke Gate                     │
│  mvn compile                              │
│  Fails fast in < 60s on compile errors    │
└───────────────────┬───────────────────────┘
                    │ on success
                    ▼
┌───────────────────────────────────────────┐
│  Stage 2 — Unit Tests                     │
│  AppointmentServiceTest                   │
│  AuthServiceTest                          │
│  DoctorServiceTest                        │
│  No MySQL required                        │
└───────────────────┬───────────────────────┘
                    │ on success
                    ▼
┌───────────────────────────────────────────┐
│  Stage 3 — Integration & API Tests        │
│  FullIntegrationTest                      │
│  ApiContractTest                          │
│  DefectLifecycleRegressionTest            │
│  AppointmentControllerTest                │
│  Maven Failsafe verification              │
│  All against H2 in-memory                │
└───────────────────┬───────────────────────┘
                    │ on success (main branch only)
                    ▼
┌───────────────────────────────────────────┐
│  Stage 4 — Build & Upload JAR             │
│  mvn package -DskipTests                  │
│  Artifact retained for 30 days            │
└───────────────────────────────────────────┘
```

> Test reports (`surefire-reports/`, `failsafe-reports/`) are uploaded as GitHub Actions artifacts after each stage so failures can be inspected without re-running the build.

---

## 🐛 Known Bugs Fixed

All bugs listed below have corresponding regression tests in `DefectLifecycleRegressionTest` or the service unit tests to permanently prevent recurrence.

| Bug ID | Severity | Description | Fix Location |
|---|---|---|---|
| BUG-001 | 🔴 HIGH | Past-date appointments could be booked | `AppointmentService.bookAppointment()` |
| BUG-002 | 🔴 HIGH | Patient could view another patient's appointment | `AppointmentService.getAppointmentByIdForPatient()` |
| BUG-003 | 🔴 HIGH | Double-booking the same slot was not prevented | `AppointmentService.isSlotAvailable()` |
| BUG-004 | 🟡 MEDIUM | Passwords shorter than 6 characters were accepted on password change | `AuthService.changePassword()` |
| BUG-005 | 🟢 LOW | Currency field was blank in payment orders | `PaymentRequest.getCurrency()` default value |
| BUG-006 | 🟡 MEDIUM | Duplicate email registration returned HTTP 500 instead of a user-friendly error | `GlobalExceptionHandler` + `AuthService.registerUser()` |
| BUG-007 | 🔴 HIGH | Completed appointments could be cancelled | `AppointmentService.cancelAppointment()` status guard |
| BUG-008 | 🔴 HIGH | Protected endpoints were accessible without a JWT token | `SecurityConfig` filter chain |
| BUG-009 | 🔴 HIGH | DOCTOR role could call patient-only booking endpoints | `SecurityConfig` role-based route guards |

---

## 📄 License

Internal project — all rights reserved.
