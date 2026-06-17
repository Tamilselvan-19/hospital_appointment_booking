#                                                   🏥 Hospital Appointment Booking System

<div align="center">

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Frontend-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![IntelliJ](https://img.shields.io/badge/IntelliJ%20IDEA-IDE-000000?style=for-the-badge&logo=intellijidea&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**A full-stack hospital appointment booking system where patients can browse doctors, book appointments by date and time slot, and doctors can manage their schedule and appointments — built with Spring Boot, Spring Security (JWT), JPA/Hibernate, MySQL, and Thymeleaf.**

[Features](#-features) · [Architecture](#-system-architecture) · [Quick Start](#-quick-start) · [Usage](#-usage) · [Project Structure](#-project-structure)

</div>

---

## 📌 Overview

The **Hospital Appointment Booking System** is a full-stack Spring Boot web application that streamlines the process of scheduling medical appointments. It provides:

- **Patient Portal** — Register, log in, browse doctors, and manage personal appointments
- **Doctor Portal** — Manage profile, set weekly availability, and handle incoming appointments
- **Appointment Engine** — Real-time slot availability, booking, confirmation, completion, and cancellation
- **JWT-based Authentication** — Stateless, secure, role-based access (Doctor / Patient)
- **Payments Module** — Cashfree-ready payment records for appointments and subscriptions
- **Responsive UI** — Bootstrap 5 + Thymeleaf templates, accessible from any browser

---

## ✨ Features

| Feature | Details |
|---|---|
| 👤 **Registration & Login** | JWT-secured sign-up/login for Doctors and Patients |
| 🔍 **Doctor Directory** | Browse and search doctors by name or specialization |
| 📅 **Smart Appointment Booking** | 4-step flow — pick doctor, date, available time slot, confirm |
| 📋 **Appointment Management** | Patients can view, view details, and cancel bookings |
| 🩺 **Doctor Schedule** | Doctors set their own working days and available time slots |
| ✅ **Appointment Lifecycle** | Confirm, complete (with diagnosis/prescription), mark no-show, or cancel |
| 💳 **Payments** | Payment record tracking for consultations and subscriptions |
| 🔐 **Role-Based Access** | Stateless JWT auth with `DOCTOR` and `PATIENT` roles |
| 🌐 **Web Interface** | Server-rendered Thymeleaf pages with Bootstrap 5 styling |

---

## 🏗️ System Architecture

```
Browser (Patient / Doctor)
         │
         ▼
   Thymeleaf + Bootstrap Frontend
   ┌──────────────────────────────┐
   │  Login / Register             │
   │  Doctor Search & Booking UI   │
   │  Doctor Schedule & Dashboard  │
   │  Patient Dashboard & Profile  │
   └──────────────────────────────┘
         │  HTTP / JSON (JWT Bearer)
         ▼
   Spring Boot Backend
   ┌──────────────────────────────┐
   │  AuthController               │
   │  DoctorController             │
   │  PatientController            │
   │  AppointmentController        │
   │  PaymentController            │
   │  ViewController                │
   │  JwtAuthenticationFilter       │
   └──────────────────────────────┘
         │
         ▼
   Service Layer
   ┌──────────────────────────┐
   │  AuthService              │
   │  DoctorService            │
   │  PatientService           │
   │  AppointmentService       │
   │  PaymentService           │
   └──────────────────────────┘
         │  Spring Data JPA / Hibernate
         ▼
   MySQL Database
   ┌──────────────────────────┐
   │  users                    │
   │  doctors                  │
   │  patients                 │
   │  appointments             │
   │  payments                 │
   └──────────────────────────┘
```

---

## ⚡ Quick Start

### Prerequisites

- Java JDK 17+
- Maven 3.8+
- MySQL 8.0+ (running on `localhost:3306`)
- IntelliJ IDEA (recommended)

### 1. Clone the repository

```bash
git clone https://github.com/Tamilselvan-19/Hospital_Appointment-Booking_System.git
cd Hospital_Appointment-Booking_System/hospital-booking-system
```

### 2. Create the database

```sql
CREATE DATABASE IF NOT EXISTS hospital_booking
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 3. Configure database credentials

Edit `src/main/resources/application.properties`, or set environment variables:

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `root` | MySQL password |
| `JWT_SECRET` | (base64 key) | JWT signing key |

### 4. Open in IntelliJ IDEA

1. Open IntelliJ IDEA
2. **File → Open** and select the `hospital-booking-system/` folder
3. Let Maven download dependencies and index the project

### 5. Run the application

```bash
# Option 1: Default credentials (root/root)
mvn spring-boot:run

# Option 2: Custom DB credentials
DB_USERNAME=myuser DB_PASSWORD=mypass mvn spring-boot:run

# Option 3: Package and run the jar
mvn clean package -DskipTests
java -jar target/hospital-booking-system-1.0.0.jar

# Or simply run HospitalBookingApplication.java from IntelliJ
```

### 6. Access

```
http://localhost:8080
```

Spring Boot + Hibernate (`ddl-auto=update`) will auto-create all tables on first run.

---

## 🖥️ Usage

1. **Register** at `/register` as a **Doctor** or **Patient**
2. **Login** at `/login` → redirected to your role-specific dashboard
3. **Patients**:
   - Browse doctors at `/patient/doctors`
   - Book an appointment via the 4-step flow at `/patient/book-appointment` (doctor → date → time slot → confirm)
   - View/cancel bookings at `/patient/my-appointments`
4. **Doctors**:
   - Set weekly availability (days & time slots) at `/doctor/schedule`
   - Manage appointments at `/doctor/appointments` — confirm, complete (add diagnosis/prescription), mark no-show, or cancel
   - Update profile details at `/doctor/profile`

### Demo Accounts

| Role | Email | Password |
|---|---|---|
| Doctor | `doctor@hospital.com` | `doctor123` |
| Patient | `patient@hospital.com` | `patient123` |

---

## 📁 Project Structure

```
hospital-booking-system/
├── src/
│   └── main/
│       ├── java/com/hospital/booking/
│       │   ├── config/         # Security, CORS, data initializer
│       │   ├── controller/      # REST + view controllers
│       │   ├── dto/             # Request/response DTOs
│       │   ├── entity/          # JPA entities (User, Doctor, Patient, Appointment, Payment)
│       │   ├── exception/       # Global exception handling
│       │   ├── repository/      # Spring Data JPA repositories
│       │   ├── security/        # JWT filter, UserDetails
│       │   └── service/         # Business logic
│       └── resources/
│           ├── templates/       # Thymeleaf HTML pages
│           │   ├── doctor/       # Doctor dashboard, profile, schedule, appointments
│           │   └── patient/      # Patient dashboard, doctors, booking, appointments, profile
│           └── application.properties
├── pom.xml
└── README.md
```

---

## 📊 Pages Overview

| Page | Role | Description |
|---|---|---|
| `index.html` | Public | Landing / home page |
| `register.html` | Public | Sign-up as Doctor or Patient |
| `login.html` | Public | Login page |
| `dashboard` (router) | Doctor/Patient | Redirects to role-specific dashboard |
| `patient/doctors.html` | Patient | Browse/search doctor directory |
| `patient/book-appointment.html` | Patient | 4-step appointment booking form |
| `patient/my-appointments.html` | Patient | View/cancel bookings |
| `patient/profile.html` | Patient | Edit profile & change password |
| `doctor/dashboard.html` | Doctor | Upcoming appointments overview |
| `doctor/schedule.html` | Doctor | Set available days & time slots |
| `doctor/appointments.html` | Doctor | Confirm, complete, cancel, no-show |
| `doctor/profile.html` | Doctor | Edit doctor profile details |

---

## 🔮 Future Improvements

- 📧 Email/SMS confirmation notifications on booking
- 💊 Richer patient medical history and prescription records
- 💳 Live Cashfree payment gateway integration
- 📱 Mobile-responsive PWA (Progressive Web App)
- 🔔 Automated appointment reminder notifications
- ⭐ Doctor ratings & review system

---

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'Add your feature'`
4. Push and open a Pull Request

---

## 👤 Author

**Tamilselvan** — [@Tamilselvan-19](https://github.com/Tamilselvan-19)

---

<div align="center">
  <sub>Built with ❤️ using Java · Spring Boot · Spring Security · MySQL · Thymeleaf · Bootstrap 5</sub>
</div>
#   h o s p i t a l _ a p p o i n t m e n t _ b o o k i n g  
 