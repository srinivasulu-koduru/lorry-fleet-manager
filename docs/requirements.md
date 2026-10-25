# System Requirements Specification (SRS): Lorry Fleet Manager

**Document ID:** LFM-DOC-002  
**Version:** 1.0.0 (Stage 1 Design)  
**Status:** DRAFT - PENDING OWNER REVIEW  
**Last Updated:** 2026-10-24  

---

## 1. Functional Requirements (FR)

### 1.1 Authentication & Account Governance (FR-AUTH)
* **FR-AUTH-01 (Owner Account Provisioning):** The initial owner account shall be provisioned securely via a dedicated backend command-line tool, authorized database bootstrap script, or secure admin invocation. There shall be **no public self-registration** endpoint for owners.
* **FR-AUTH-02 (Driver Account Lifecycle):** The owner shall have the sole authority to create permanent driver accounts (username, temporary password, full name, phone number, driving license number). Driver accounts are permanent and **shall never be automatically deleted or de-provisioned** when a trip completes.
* **FR-AUTH-03 (Authentication Mechanism):** The backend shall authenticate users using industry-standard password verification (BCrypt work factor 10+) and issue stateless JWT bearer tokens with configurable expiration (e.g., 7-day mobile session with refresh capability).
* **FR-AUTH-04 (Account Status Control):** The owner shall have the authority to activate or deactivate any driver account. Deactivated drivers shall be rejected immediately at login or on subsequent authenticated API calls.
* **FR-AUTH-05 (Password Self-Service):** Both owner and driver users shall be able to change their account password after authenticating, providing their current password and a compliant new password.
* **FR-AUTH-06 (Session Persistence & Logout):** The Android app shall securely store authentication tokens (using Android Jetpack Security EncryptedSharedPreferences) to maintain persistent login across app restarts until explicit logout or token invalidation.

---

### 1.2 Vehicle Fleet Management (FR-FLEET)
* **FR-FLEET-01 (Lorry Registry):** The owner shall be able to register, view, update, and inspect lorry profiles. Required attributes include:
  - Vehicle Registration Number (e.g., `AP 26 XX 1234` - unique identifier)
  - Make and Model (e.g., Ashok Leyland 2820 / 3518)
  - Gross Vehicle Weight / Carrying Capacity in Metric Tons
  - Telematics Device Identifier (iALERT Unit ID or GPS IMEI)
  - Operational Status (`AVAILABLE`, `ON_TRIP`, `MAINTENANCE`, `UNAVAILABLE`)
* **FR-FLEET-02 (Turnaround Availability Gating):** When a trip is completed, the vehicle's operational status **shall not** automatically switch to `AVAILABLE`. The owner must explicitly evaluate vehicle fitness and assign its next status (`AVAILABLE`, `MAINTENANCE`, or `UNAVAILABLE`).
* **FR-FLEET-03 (Dispatch Eligibility Enforcement):** The system shall block trip assignment for any lorry whose operational status is not strictly `AVAILABLE`. Lorries under `MAINTENANCE`, `UNAVAILABLE`, or currently `ON_TRIP` shall be ineligible for selection.

---

### 1.3 Trip Planning & Dispatch (FR-TRIP)
* **FR-TRIP-01 (Trip Creation):** The owner shall be able to create new trips specifying:
  - Unique System Trip Code (e.g., `TRP-2026-0001`)
  - Lorry selection (from eligible `AVAILABLE` lorries only)
  - Origin / Starting Location (Text name and optional landmark)
  - Destination Location (Text name, with target latitude/longitude coordinates)
  - Scheduled Start Date / Time
  - Freight Trip Amount (in INR ₹ - strictly owner-confidential)
  - Assigned Driver(s) (One primary driver, and optionally one or more co-drivers)
  - Special Instructions / Cargo Notes
* **FR-TRIP-02 (Multi-Driver Assignment):** The system shall support assigning multiple drivers to a single trip, designating their roles (`PRIMARY_DRIVER` vs `CO_DRIVER`).
* **FR-TRIP-03 (Driver Conflict Prevention):** The system shall warn or prevent assigning a driver who is already actively assigned to an ongoing `IN_PROGRESS` trip.
* **FR-TRIP-04 (Driver Trip Visibility):** Drivers shall view only trips to which they are explicitly assigned, categorized into:
  - Active Trip (currently running)
  - Upcoming Trips (assigned, scheduled for future start)
  - Completed Trip History

---

### 1.4 Trip Lifecycle & Geofenced Arrival Verification (FR-GEO)
* **FR-GEO-01 (Trip Progression):** Trips shall follow a defined state progression:  
  `ASSIGNED` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `DESTINATION_REACHED_REQUESTED` $\rightarrow$ `AWAITING_OWNER_CONFIRMATION` $\rightarrow$ `COMPLETED` (or `CANCELLED`).
* **FR-GEO-02 (Destination Arrival Request):** When arriving at the destination, the assigned driver shall submit a "Reached Destination" request via the mobile application.
* **FR-GEO-03 (500-Metre Geofence Verification):** Upon receiving the arrival request, the mobile client shall sample current GPS coordinates and submit them to the backend:
  - The backend calculates the Haversine distance between current coordinates and target destination coordinates.
  - If calculated distance $\le 500\text{ metres}$, the check passes as verified.
  - If calculated distance $> 500\text{ metres}$, the verification fails with a descriptive explanation (e.g., "Current location is 1.8 km from scheduled destination").
* **FR-GEO-04 (Telemetry Integrity & Staleness Protection):** GPS readings older than 3 minutes or with horizontal accuracy worse than 100 metres shall be flagged as inaccurate/stale. The client shall be prompted to retry with an active satellite lock.
* **FR-GEO-05 (Manual Review Fallback):** If GPS is unavailable (e.g., deep warehouse dead-zone, hardware fault, driver phone GPS disabled), the driver shall be permitted to submit an arrival request with an mandatory explanation note. The trip enters `AWAITING_OWNER_CONFIRMATION` with a clear "Manual Review Required / GPS Unverified" warning flag.
* **FR-GEO-06 (Owner Confirmation Gate):** The trip shall not transition to `COMPLETED` until the owner explicitly inspects the arrival submission (GPS audit trail or manual note) and approves completion.

---

### 1.5 Financial Management & Privacy (FR-FIN)
* **FR-FIN-01 (Driver Financial Isolation - Core Security Rule):** Drivers shall have **zero access** to any financial data. Trip freight amounts, income entries, expense entries, maintenance costs, and profit calculations must be strictly omitted from all driver DTOs and guarded by `@PreAuthorize("hasRole('OWNER')")` on the backend.
* **FR-FIN-02 (Income Tracking):** The owner shall record freight income linked to a specific lorry and trip (or general freight income) with fields: date, lorry, amount, payer/party details, receipt number, and notes.
* **FR-FIN-03 (Expense Categorization):** The owner shall record vehicle operating expenses categorized into:
  - `DIESEL` (litres, fuel station, amount)
  - `TOLL` (FASTag recharge or toll booth cash)
  - `REPAIRS` (breakdown repairs, mechanical work)
  - `TYRES` (puncture, retreading, replacement)
  - `SERVICING` (scheduled lubrication, filters, general service)
  - `OTHER` (driver batta/allowance, loading/unloading, parking, tax/permit)
* **FR-FIN-04 (Maintenance Records):** The owner shall maintain vehicle service records including: date, lorry, maintenance description, workshop/garage name, total cost, odometer reading at service, and recommended next service date/odometer milestone.

---

### 1.6 Financial Reporting & Document Sharing (FR-REP)
* **FR-REP-01 (Monthly P&L Calculation):** The system shall compute monthly financial summaries:
  $$\text{Net Profit / Loss} = \sum \text{Total Income} - \sum \text{Total Expenses}$$
* **FR-REP-02 (Fleet & Vehicle Aggregation):** The owner shall be able to filter financial reports by month/year and view:
  - Fleet-wide consolidated income, expenses, and net profit.
  - Per-lorry breakdown (Income vs Expenses per vehicle).
  - Expense category distribution (percentage spent on Diesel vs Toll vs Maintenance).
* **FR-REP-03 (PDF Generation & Sharing):** The system shall generate a clean, branded PDF summary containing vehicle performance, expense breakdown, and profit figures. The Android app shall invoke the standard Android Share Sheet (`Intent.ACTION_SEND`) to share the generated PDF via WhatsApp, Email, or cloud storage.

---

## 2. Non-Functional Requirements (NFR)

### 2.1 Security & Data Isolation (NFR-SEC)
* **NFR-SEC-01 (Backend Authorization):** All business endpoints must validate user role via Spring Security method annotations. Relying solely on client UI hiding is strictly prohibited.
* **NFR-SEC-02 (Credential Protection):** All user passwords shall be hashed using BCrypt (work factor 10+). Plaintext passwords must never appear in logs, responses, or storage.
* **NFR-SEC-03 (Secret Hygiene):** Database credentials, JWT signing keys, and third-party API keys shall be injected strictly via environment variables. No secrets shall be committed to Git.
* **NFR-SEC-04 (Error Information Leakage):** The API must return sanitized JSON error envelopes (`ErrorResponse`) without Java stack traces, database schema details, or SQL exception dumps (`server.error.include-stacktrace: never`).

### 2.2 Performance & Scalability (NFR-PERF)
* **NFR-PERF-01 (Target User Concurrency):** Optimized for 15 active users and 5 lorries, keeping backend memory footprint under 384 MB RAM to operate within cloud free-tier constraints.
* **NFR-PERF-02 (API Response Latency):** Core transactional endpoints (auth, trip status update, expense entry) shall respond within 200ms under standard mobile 4G/LTE conditions (excluding cold start latency on free-tier platforms).
* **NFR-PERF-03 (Payload Optimization):** Mobile payloads must be lightweight JSON, minimizing data transfer for drivers operating on remote highways with limited cellular reception.

### 2.3 Usability & Mobile Ergonomics (NFR-UX)
* **NFR-UX-01 (Driver UI Simplicity):** The driver interface must feature large touch targets ($\ge 48\times 48\,\text{dp}$), high-contrast text, clear visual cards, and minimal textual input requirements.
* **NFR-UX-02 (Owner Dashboard Clarity):** The owner dashboard must provide at-a-glance fleet status KPI cards (Active Lorries, Available Lorries, Lorries in Maintenance, Pending Confirmations).

### 2.4 Reliability & Robustness (NFR-REL)
* **NFR-REL-01 (Network Resilience):** The Android app must gracefully handle connectivity loss, timeout gracefully, and inform the user with actionable retry prompts rather than crashing.
* **NFR-REL-02 (Idempotency):** Status update requests (such as destination reached submissions) shall be idempotent to avoid duplicate transition events if a driver taps repeatedly during poor connectivity.

---

## 3. Assumptions vs. Unresolved Decisions Matrix

| Item Code | Category | Requirement / Decision Topic | Current Assumption | Requires Owner Clarification |
| :--- | :--- | :--- | :--- | :--- |
| **Q-01** | GPS | Destination Geocoding Method | Owner inputs text city + manual Lat/Lng or chooses from map pin. | Confirm whether owner wants open map picker (e.g., OpenStreetMap) or predefined city drop-downs. |
| **Q-02** | GPS | Ashok Leyland iALERT Integration | Assume official iALERT API subscription will be pursued; driver phone GPS is fallback. | Confirm whether owner has commercial API credentials from Ashok Leyland and associated data cost limits. |
| **Q-03** | Trip | Driver Trip Acceptance Step | Drivers can directly start an `ASSIGNED` trip to `IN_PROGRESS`. | Confirm whether a formal "Acknowledge / Accept Trip" button is needed before departure. |
| **Q-04** | Push | Notifications Infrastructure | Using in-app dashboard badges during pilot, with Firebase Cloud Messaging (FCM) planned for background alerts. | Confirm whether push notifications while app is closed are required immediately in Stage 2/3. |
| **Q-05** | Financial | Driver Expense Logging | Currently expenses are logged exclusively by the Owner. | Confirm whether drivers should ever be allowed to submit on-road diesel/toll receipts for owner review. |
