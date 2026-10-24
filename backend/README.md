# Lorry Owner Backend

Backend foundation service for the private **Lorry Owner** Android application (Kotlin & Jetpack Compose).

---

## 1. Project Overview

The **Lorry Owner Backend** provides a secure, production-minded REST API foundation built to support fleet management operations for lorry owners. This backend is designed specifically for mobile clients, enforcing stateless security, strict input validation, safe error handling, and robust database abstraction.

In this foundational phase:
- The base Spring Boot 3 architecture is established.
- Spring Security is configured with stateless sessions, BCrypt password hashing, and least-privilege endpoint protection.
- JPA and MySQL configuration are prepared to read securely from environment variables.
- A public health verification endpoint (`GET /api/health`) is provided.
- Comprehensive automated tests ensure the core foundation functions reliably.

---

## 2. Requirements

- **Java Development Kit (JDK)**: Java 21 LTS
- **Build Tool**: Maven 3.9+ (or the included Maven Wrapper `mvnw` / `mvnw.cmd`)
- **Database**: MySQL 8.0+

To verify your Java installation in PowerShell:
```powershell
java --version
javac --version
```
Ensure that `JAVA_HOME` points to your JDK 21 installation directory.

---

## 3. Technology Stack & Dependencies

| Dependency | Purpose | Why It Was Chosen |
| :--- | :--- | :--- |
| **Java 21 LTS** | Language Runtime | Long-term support, modern language features (records, pattern matching, virtual threads). |
| **Spring Boot 3.3.4** | Core Application Framework | Stable, production-ready framework with native Java 21 support and auto-configuration. |
| **Spring Web (`spring-boot-starter-web`)** | REST API & Web MVC | Standard REST controller support, embedded Tomcat web server, JSON serialization via Jackson. |
| **Spring Security (`spring-boot-starter-security`)** | Security & Access Control | Industry-standard security filter chain, BCrypt password hashing, and role-based access readiness. |
| **Spring Data JPA (`spring-boot-starter-data-jpa`)** | Persistence & ORM | Simplifies database interactions via Hibernate ORM and declarative repository interfaces. |
| **MySQL Connector/J (`mysql-connector-j`)** | Database Driver | Official JDBC driver for communicating with MySQL databases. |
| **Bean Validation (`spring-boot-starter-validation`)** | Input Validation | Jakarta Bean Validation constraints (`@NotNull`, `@Size`, etc.) to validate client request payloads. |
| **Spring Boot Actuator (`spring-boot-starter-actuator`)** | Application Monitoring | Exposes health indicators and application status safely without leaking internal secrets. |
| **Spring Boot Test (`spring-boot-starter-test`)** | Automated Testing | JUnit 5 Jupiter engine, MockMvc web slice testing, and AssertJ fluent assertions. |
| **Spring Security Test (`spring-security-test`)** | Security Testing | Enables testing security filters, authentication challenges, and access rules. |

---

## 4. Backend Project Structure

```
backend/
├── pom.xml                               # Maven project configuration and dependencies
├── mvnw                                  # Maven wrapper script for Linux/macOS
├── mvnw.cmd                              # Maven wrapper script for Windows
├── .gitignore                            # Backend-specific ignore rules (build artifacts, secrets)
├── .env.example                          # Safe environment variable configuration template
├── README.md                             # Backend documentation and guide
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── lorryowner/
│   │   │           └── backend/
│   │   │               ├── LorryOwnerBackendApplication.java   # Spring Boot entry point
│   │   │               ├── config/                             # Application configurations
│   │   │               ├── controller/                         # REST API controllers (HealthController)
│   │   │               ├── dto/                                # Data transfer objects (HealthResponse, ErrorResponse)
│   │   │               ├── entity/                             # JPA entities (to be added in future phases)
│   │   │               ├── exception/                          # Global exception handling (GlobalExceptionHandler)
│   │   │               ├── repository/                         # Spring Data repositories (future phases)
│   │   │               ├── security/                           # Spring Security & BCrypt configuration (SecurityConfig)
│   │   │               └── service/                            # Business service layer (future phases)
│   │   └── resources/
│   │       └── application.yml                   # Production-safe YAML configuration with env placeholders
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── lorryowner/
│       │           └── backend/
│       │               ├── LorryOwnerBackendApplicationTests.java # Context loading verification
│       │               ├── controller/
│       │               │   └── HealthControllerTest.java          # Health endpoint assertions
│       │               ├── exception/
│       │               │   └── GlobalExceptionHandlerTest.java    # Error handling verification
│       │               └── security/
│       │                   └── SecurityConfigTest.java            # Security and BCrypt tests
│       └── resources/
│           └── application.yml                   # Test profile configuration
```

---

## 5. MySQL Database Configuration

### Step 1: Create Database and Dedicated User

Run the following SQL commands in your MySQL client (e.g., MySQL Command Line Client, MySQL Workbench, or phpMyAdmin):

```sql
-- Create the database with UTF-8 character encoding
CREATE DATABASE IF NOT EXISTS lorry_fleet_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Create a dedicated application user (replace 'your_strong_password' with a secure password)
CREATE USER IF NOT EXISTS 'lorry_app_user'@'localhost' IDENTIFIED BY 'your_strong_password';

-- Grant required privileges on the database to the application user
GRANT ALL PRIVILEGES ON lorry_fleet_db.* TO 'lorry_app_user'@'localhost';

-- Apply privilege changes
FLUSH PRIVILEGES;
```

> **Important**: Do not use the `root` administrative account for daily application operations. Creating a dedicated application user (`lorry_app_user`) follows the security principle of least privilege.

### Step 2: Conservative Schema Management

In `application.yml`, JPA schema generation is configured to:
```yaml
spring.jpa.hibernate.ddl-auto: validate
```
- `validate` strictly verifies that existing tables match entity mappings without dropping or modifying database tables.
- **Never** set `ddl-auto` to `create` or `create-drop` in production or shared staging environments, as doing so drops existing tables and causes permanent data loss.

---

## 6. Required Environment Variables

The application reads all database credentials and server settings from environment variables. Placeholders are defined in `application.yml`.

| Variable | Description | Example / Default |
| :--- | :--- | :--- |
| `PORT` | HTTP server port | `8080` |
| `DB_URL` | Full JDBC connection URL for MySQL | `jdbc:mysql://localhost:3306/lorry_fleet_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USERNAME` | MySQL database username | `lorry_app_user` |
| `DB_PASSWORD` | MySQL database password | `your_strong_password` |

### Setting Environment Variables in PowerShell

#### Option A: Set for the Current PowerShell Session
```powershell
$env:PORT = "8080"
$env:DB_URL = "jdbc:mysql://localhost:3306/lorry_fleet_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "lorry_app_user"
$env:DB_PASSWORD = "your_strong_password"
```

#### Option B: Using `.env.example`
A reference template is available at `backend/.env.example`. You can copy it or reference it when configuring local or production secrets.

---

## 7. How to Build and Run

All commands should be executed from the `backend/` directory in PowerShell.

### 1. Build the Application
```powershell
Set-Location -Path "D:\lorry-fleet-manager\backend"
.\mvnw.cmd clean package
```

### 2. Run the Application
```powershell
# Using the Spring Boot Maven Plugin
.\mvnw.cmd spring-boot:run

# Or running the packaged JAR directly
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

---

## 8. How to Run Automated Tests

To execute the automated test suite:
```powershell
Set-Location -Path "D:\lorry-fleet-manager\backend"
.\mvnw.cmd test
```

### What the Automated Tests Verify:
1. **`LorryOwnerBackendApplicationTests`**: Verifies that the Spring Boot application context boots up successfully.
2. **`HealthControllerTest`**: Verifies that `GET /api/health` returns HTTP `200 OK` and the exact JSON payload `{"status":"UP","application":"Lorry Owner Backend"}`.
3. **`SecurityConfigTest`**:
   - Confirms `GET /api/health` is publicly accessible without credentials.
   - Confirms that unauthenticated requests to protected endpoints return HTTP `401 Unauthorized` with a structured JSON error body.
   - Confirms that `PasswordEncoder` is an instance of `BCryptPasswordEncoder` and correctly hashes and verifies passwords.
4. **`GlobalExceptionHandlerTest`**: Confirms that invalid HTTP methods return HTTP `405 Method Not Allowed` with clean error messages and no stack traces.

---

## 9. Testing the Health Check Endpoint

### Using PowerShell `Invoke-RestMethod`
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method Get | ConvertTo-Json
```

### Using `curl`
```powershell
curl.exe -i http://localhost:8080/api/health
```

### Expected Response
```json
{
  "status": "UP",
  "application": "Lorry Owner Backend"
}
```

---

## 10. Security Precautions

1. **No Hardcoded Secrets**: Database credentials, passwords, and sensitive settings are strictly read from environment variables.
2. **Stateless REST Communication**: Sessions are stateless (`SessionCreationPolicy.STATELESS`), suitable for mobile clients and token-based authentication.
3. **No Information Leakage**: Spring's default stack trace and server message exposure are disabled (`server.error.include-stacktrace: never`). The `GlobalExceptionHandler` logs exceptions internally on the server and returns clean, sanitized JSON errors to the client.
4. **Strong Password Hashing**: Passwords must always be hashed with the configured `BCryptPasswordEncoder` bean before storage. Plaintext passwords are never stored.
5. **Least-Privilege Endpoint Security**: Only explicitly registered endpoints (such as `/api/health`) allow anonymous access. All other endpoints require authentication by default.

---

## 11. Known Limitations & Next Steps

This stage establishes only the foundational backend architecture. In accordance with scope control:

### What Is Deferred to Future Phases:
- **Authentication System**: User registration, login, and token generation (e.g. JWT) for `ROLE_OWNER` and `ROLE_DRIVER`.
- **Domain Entities**: Tables and JPA entities for Lorry, Driver, Trip, Expense, and Income records.
- **Business Endpoints**: CRUD operations for fleet vehicles, trip logs, GPS tracking, and financial calculations.
- **Reporting**: Monthly expense/income reports and PDF exports.

These modules will be implemented incrementally after reviewing this backend foundation and agreeing on the data model design.
