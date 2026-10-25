# Master Project Progress & Roadmap: Lorry Fleet Manager

**Project Root:** `D:\lorry-fleet-manager`  
**GitHub Repository:** `https://github.com/srinivasulu-koduru/lorry-fleet-manager.git`  
**Current Milestone:** Stage 2 — Backend Authentication + Owner Setup + Android Login Integration  
**Current Status:** **STAGE 2 COMPLETE & VERIFIED END-TO-END**  
**Last Updated:** 2026-10-24  

---

## 1. Executive Dashboard

| Dimension | Current State | Notes |
| :--- | :--- | :--- |
| **Current Stage** | **Stage 2: Backend Auth + Owner Setup + Android Login** | Backend JWT auth, owner bootstrap, secure mobile storage, live emulator verified. |
| **Backend State** | Spring Boot 3.3.4 (Java 21, Spring Security, JJWT) | 27 automated tests passing (100%); live MySQL 8.4 database connected with `ddl-auto: validate`. |
| **Database State** | MySQL 8.4 (`lorry_fleet_db`) | `users` and `lorries` tables provisioned with foreign keys, indexes, and BCrypt hashes. |
| **Android App State**| Jetpack Compose Mobile App | Visual login screen recreated accurately; Keystore `SessionManager` integrated; cold restart session restoration verified. |
| **Verification State**| **100% End-to-End Verified** | Owner login -> Owner Dashboard; Driver login -> Driver Dashboard; Sign Out -> Login Screen. |
| **Next Stage** | Stage 3: Fleet (Lorry) Management & Driver Account Onboarding UI | CRUD operations for 5 lorries and driver management screens. |

---

## 2. Stage 2 Implementation Summary

### A. Backend Architecture & Authentication (Completed)
- **Unified Auth Endpoint (`POST /api/auth/login`):**
  - Single public endpoint serving both `OWNER` and `DRIVER` roles.
  - Generates HMAC-SHA256 signed JWTs with claims (`userId`, `role`, `pwdVer`, `exp`).
  - Strict authentication failure responses (401 Unauthorized with clean JSON error; zero information leakage).
- **Secure Initial Owner Bootstrap (`OwnerBootstrapService`):**
  - Automated `ApplicationRunner` provisioning the first owner via environment variables (`INITIAL_OWNER_USERNAME`, `INITIAL_OWNER_PASSWORD`).
  - Refuses to overwrite existing owner accounts (`existsByRole(Role.OWNER)` check).
  - Skips safely without crashing if credentials are not configured.
  - BCrypt password hashing; plaintext passwords are never stored or logged.
- **Owner-Exclusive Driver Management (`POST/GET /api/owner/drivers`):**
  - Role-gated via Spring Security `@PreAuthorize("hasRole('OWNER')")`.
  - Creates permanent driver credentials and returns driver profiles.
  - Driver role attempting access receives strict 403 Forbidden.
- **User Self-Service (`GET /api/users/me`, `POST /api/users/change-password`):**
  - Retrieves authenticated user profile.
  - Password change requires current password validation, enforces complexity (min 8 chars, uppercase, lowercase, digit), and invalidates all prior JWT tokens via `pwdVer` timestamp comparison.
- **Automated Backend Test Suite:**
  - 27 unit and integration tests passing (`AuthControllerTest`, `UserControllerTest`, `OwnerDriverControllerTest`, `OwnerBootstrapServiceTest`, `SecurityConfigTest`, `HealthControllerTest`, `GlobalExceptionHandlerTest`).
  - Isolated test execution against H2 in-memory MySQL emulation mode.

### B. Database Schema & Validation (Completed)
- **Database Engine:** MySQL 8.4 on localhost:3306 (`lorry_fleet_db`).
- **Strict Hibernate Setting:** Retained `ddl-auto: validate` in production configuration.
- **Executed Migration Script:** `backend/database/schema-stage2.sql`.
  - Table `users`: id, username (UNIQUE), password_hash, role, full_name, phone_number, license_number, is_active, password_updated_at, created_at, updated_at.
  - Table `lorries`: id, registration_number (UNIQUE), make_model, capacity_tons, operational_status, ialert_unit_id, notes, created_at, updated_at.

### C. Android Jetpack Compose Integration (Completed)
- **BHP Logistics Visual Screen Recreation:**
  - High-fidelity visual match to the reference design.
  - Dedicated photographic asset `bhp_truck_highway.png` with overlaid white truck logo, brand typography, and curved white card header.
  - Clean Material 3 inputs with dark forest green accenting, custom vector icons, password visibility toggle, and error banner.
- **Android Keystore Session Management (`SessionManager`):**
  - Uses `EncryptedSharedPreferences` backed by `MasterKey` (AES256_GCM).
  - Persists access token, user ID, username, and trusted backend role (`OWNER` or `DRIVER`).
  - Automatic cold restart session restoration: launches directly into Owner or Driver dashboard without re-prompting login.
  - Complete session clearance on Sign Out / Lock Portal.
- **Role-Based Navigation:**
  - Single login screen with role strictly determined by trusted backend JWT payload.
  - `OWNER` -> `OwnerDashboardScreen` ("FLEET OWNER COMMAND DASHBOARD").
  - `DRIVER` -> `DriverDashboardScreen` ("DRIVER OPERATIONS PORTAL").
- **Network Resilience:**
  - Automatically resolves localhost across emulator virtual gateways (`10.0.2.2`, `127.0.0.1` via ADB reverse forwarding).

---

## 3. Operational Setup Guide: Initial Owner Provisioning

To start the backend and configure your initial Owner account:

1. **Set Environment Variables in PowerShell:**
   ```powershell
   $env:DB_USER = "YOUR_DATABASE_USERNAME"
   $env:DB_PASS = "YOUR_DATABASE_PASSWORD"
   $env:INITIAL_OWNER_USERNAME = "YOUR_OWNER_USERNAME"
   $env:INITIAL_OWNER_PASSWORD = "YOUR_OWNER_PASSWORD"
   $env:INITIAL_OWNER_FULL_NAME = "Fleet Administrator"
   ```

2. **Start the Backend:**
   ```powershell
   cd D:\lorry-fleet-manager\backend
   .\mvnw.cmd spring-boot:run
   ```

3. **Verify Bootstrap in Logs:**
   ```text
   INITIAL OWNER ACCOUNT PROVISIONED SUCCESSFULLY:
   Username: 'YOUR_OWNER_USERNAME' | Role: OWNER
   Password was hashed with BCrypt. Plaintext credentials were not stored or logged.
   ```

4. **Launch Android App:**
   - Log in using `YOUR_OWNER_USERNAME` and `YOUR_OWNER_PASSWORD`.
   - The app verifies credentials with the backend, stores the JWT securely, and navigates to the Fleet Owner Command Dashboard.

---

## 4. Verification Evidence & Artifacts

- **Owner Authenticated Dashboard Screenshot:**  
  `[owner_dashboard_authenticated.png](file:///C:/Users/k%20srinivasulu/.gemini/antigravity/brain/e175781b-96a9-4be7-9ddd-eca2007673d1/owner_dashboard_authenticated.png)`
- **Driver Authenticated Dashboard Screenshot:**  
  `[driver_dashboard_authenticated.png](file:///C:/Users/k%20srinivasulu/.gemini/antigravity/brain/e175781b-96a9-4be7-9ddd-eca2007673d1/driver_dashboard_authenticated.png)`
- **Login Screen Visual Recreation Screenshot:**  
  `[login_visual_comparison.png](file:///C:/Users/k%20srinivasulu/.gemini/antigravity/brain/e175781b-96a9-4be7-9ddd-eca2007673d1/login_visual_comparison.png)`
- **Backend Automated Test Report:** 27 tests passing, 0 failures, 0 errors.

---

## 5. Next Milestone: Stage 3

- **Stage 3 Scope:**
  - Owner Lorry Management (CRUD for 5 commercial lorries, capacity, status: `AVAILABLE`, `MAINTENANCE`, `UNAVAILABLE`).
  - Ashok Leyland iALERT unit ID association.
  - Driver account creation UI in the Owner mobile app.
