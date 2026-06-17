# Hospital Booking System – Setup Guide

## Requirements
- Java 17+
- Maven 3.8+
- MySQL 8.0+ (running on localhost:3306)

## Database Setup

1. Start MySQL and run:
```sql
CREATE DATABASE IF NOT EXISTS hospital_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Make sure a MySQL user exists with access (default config uses `root`/`root`):
```sql
-- If using root/root (default), skip this step
-- Otherwise create a dedicated user:
CREATE USER 'hospital_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON hospital_booking.* TO 'hospital_user'@'localhost';
FLUSH PRIVILEGES;
```

## Configuration

Edit `src/main/resources/application.properties` **or** set environment variables:

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `root` | MySQL password |
| `JWT_SECRET` | (base64 key) | JWT signing key |

## Running the Application

```bash
# Option 1: Default (uses root/root)
mvn spring-boot:run

# Option 2: Custom DB credentials
DB_USERNAME=myuser DB_PASSWORD=mypass mvn spring-boot:run

# Option 3: Package and run
mvn clean package -DskipTests
java -jar target/hospital-booking-system-1.0.0.jar
```

## Access

- **Homepage**: http://localhost:8080
- **Login**: http://localhost:8080/login
- **Register**: http://localhost:8080/register

## Default Accounts (auto-created on first start)

| Role | Email | Password |
|---|---|---|
| Admin | admin@hospital.com | admin123 |
| Doctor | doctor@hospital.com | doctor123 |
| Patient | patient@hospital.com | patient123 |

## Error Pages

- **404** – Page Not Found  
- **403** – Access Denied  
- **500** – Internal Server Error  

All errors route to `/error` with a styled Thymeleaf template.

## Fixed Issues

1. **Missing `register.html`** – `/register` page now exists with full form for Patient/Doctor/Admin
2. **500 errors** – Improved GlobalExceptionHandler with specific cases (DataIntegrity, Auth, Validation)
3. **Missing ErrorController** – Custom `ErrorController` routes all HTTP errors to the styled error template
4. **403 redirect** – Security config now redirects unauthenticated → `/login`, forbidden → `/access-denied`
5. **CORS fix** – Added `127.0.0.1` origin and `PATCH` method
6. **DB connection** – Added `characterEncoding=UTF-8`, `useUnicode=true`, `connection-test-query=SELECT 1`
7. **Error template** – Enhanced `error.html` with specific messages for 400/401/403/404/500/503
