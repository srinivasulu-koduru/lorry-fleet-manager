# REST API Architecture & Interface Plan

**Document ID:** LFM-DOC-009  
**Version:** 1.0.0 (Stage 1 Design)  
**Status:** DRAFT - PENDING OWNER REVIEW  
**Last Updated:** 2026-10-24  

---

## 1. Architectural Principles

The API follows a strict RESTful architecture running over HTTPS:
1. **Stateless Security:** Authentication via standard HTTP `Authorization: Bearer <jwt-token>` header. No server-side HTTP session cookies are created (`SessionCreationPolicy.STATELESS`).
2. **Strict Domain Segregation:** Endpoints are segregated by route prefixes (`/api/owner/**`, `/api/driver/**`, `/api/users/**`, `/api/auth/**`), allowing declarative role verification in `SecurityConfig` and method security via `@PreAuthorize`.
3. **Information Leakage Prevention:** Driver DTOs do not contain commercial freight rates or fleet financial fields. The `GlobalExceptionHandler` converts all exceptions into clean JSON error envelopes (`ErrorResponse`), hiding server internals.
4. **Standard HTTP Response Codes:**
   - `200 OK` — Successful retrieval or state change.
   - `201 Created` — Resource successfully created (with `Location` header).
   - `400 Bad Request` — Validation failures (`MethodArgumentNotValidException`).
   - `401 Unauthorized` — Missing, expired, or malformed authentication token.
   - `403 Forbidden` — Authenticated user lacks required role permissions.
   - `404 Not Found` — Resource does not exist.
   - `409 Conflict` — Business rule conflict (e.g. attempting to assign a lorry whose status is `MAINTENANCE`).
   - `422 Unprocessable Entity` — Domain rule validation failed (e.g. GPS arrival verification > 500m).

---

## 2. API Endpoint Catalog

```mermaid
graph LR
    subgraph "Public Endpoints"
        A1[POST /api/auth/login]
        A2[GET /api/health]
    end

    subgraph "Authenticated Self-Service"
        B1[GET /api/users/me]
        B2[POST /api/users/change-password]
    end

    subgraph "Driver Endpoints (ROLE_DRIVER)"
        C1[GET /api/driver/trips/dashboard]
        C2[GET /api/driver/trips/{id}]
        C3[POST /api/driver/trips/{id}/start]
        C4[POST /api/driver/trips/{id}/reached-destination]
        C5[POST /api/driver/trips/{id}/manual-arrival-request]
        C6[GET /api/driver/trips/history]
    end

    subgraph "Owner Endpoints (ROLE_OWNER)"
        D1[CRUD /api/owner/lorries]
        D2[CRUD /api/owner/drivers]
        D3[CRUD /api/owner/trips]
        D4[POST /api/owner/trips/{id}/confirm]
        D5[POST /api/owner/trips/{id}/reject]
        D6[CRUD /api/owner/finance/income]
        D7[CRUD /api/owner/finance/expenses]
        D8[CRUD /api/owner/finance/maintenance]
        D9[GET /api/owner/reports/monthly]
        D10[GET /api/owner/reports/monthly/pdf]
    end
```

### 2.1 Authentication & Profile APIs

| Method | Endpoint | Role | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Public | Authenticates credentials and returns JWT bearer token + user role. |
| `POST` | `/api/auth/refresh` | Public | Exchanges a valid refresh token for a fresh access token. |
| `GET` | `/api/users/me` | Authenticated | Retrieves current authenticated user profile. |
| `POST` | `/api/users/change-password` | Authenticated | Updates current user password after verifying current credentials. |

---

### 2.2 Owner Fleet Management APIs (`ROLE_OWNER`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/owner/lorries` | Lists all 5 fleet lorries with current operational status and active trip info. |
| `POST` | `/api/owner/lorries` | Registers a new lorry into the fleet. |
| `GET` | `/api/owner/lorries/{id}` | Retrieves full specifications, telematics unit ID, and service history of a lorry. |
| `PUT` | `/api/owner/lorries/{id}` | Updates lorry metadata (make, model, capacity, notes). |
| `PATCH` | `/api/owner/lorries/{id}/status` | Manually updates operational status (`AVAILABLE`, `MAINTENANCE`, `UNAVAILABLE`). |

---

### 2.3 Owner Driver Management APIs (`ROLE_OWNER`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/owner/drivers` | Lists all permanent driver accounts with active trip and status indicators. |
| `POST` | `/api/owner/drivers` | Creates a new permanent driver account (generates username and initial credentials). |
| `GET` | `/api/owner/drivers/{id}` | Retrieves driver profile, license info, and aggregate completed trips count. |
| `PUT` | `/api/owner/drivers/{id}` | Updates driver phone number, license details, or contact information. |
| `PATCH` | `/api/owner/drivers/{id}/status` | Toggles driver status between `ACTIVE` and `INACTIVE`. |
| `POST` | `/api/owner/drivers/{id}/reset-password` | Resets a driver's password and returns a temporary credential. |

---

### 2.4 Owner Trip Management & Turnaround APIs (`ROLE_OWNER`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/owner/trips` | Lists fleet trips with optional status filter (`ASSIGNED`, `IN_PROGRESS`, etc.). |
| `POST` | `/api/owner/trips` | Creates and dispatches a trip. Enforces lorry eligibility and driver conflicts. |
| `GET` | `/api/owner/trips/{id}` | Detailed trip overview including confidential freight rate and financial records. |
| `GET` | `/api/owner/trips/pending-confirmations` | Lists all trips awaiting owner confirmation with GPS verification evidence. |
| `POST` | `/api/owner/trips/{id}/confirm` | Confirms trip completion; **requires** mandatory post-trip lorry status selection. |
| `POST` | `/api/owner/trips/{id}/reject` | Rejects arrival submission and sends trip back to `IN_PROGRESS` with rejection note. |
| `POST` | `/api/owner/trips/{id}/cancel` | Cancels an unstarted or aborted trip with mandatory reason note. |

---

### 2.5 Driver Trip APIs (`ROLE_DRIVER`)

> [!IMPORTANT]
> All driver responses are strictly mapped to `DriverTrip...Response` classes where financial attributes do not exist.

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/driver/trips/dashboard` | Returns the driver's current active trip, upcoming trips, and recent stats. |
| `GET` | `/api/driver/trips/{id}` | Retrieves permitted details for an assigned trip (route, cargo notes, dates). |
| `POST` | `/api/driver/trips/{id}/start` | Transitions assigned trip to `IN_PROGRESS` upon physical departure. |
| `POST` | `/api/driver/trips/{id}/reached-destination` | Submits device GPS coordinates for 500m geofence evaluation. |
| `POST` | `/api/driver/trips/{id}/manual-arrival-request`| Submits arrival request with mandatory explanation note when GPS check fails. |
| `GET` | `/api/driver/trips/history` | Paginated list of completed trips assigned to this driver. |

---

### 2.6 Owner Financial & Reporting APIs (`ROLE_OWNER`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/owner/finance/income` | Lists income records filtered by date range or lorry. |
| `POST` | `/api/owner/finance/income` | Records freight income received for a lorry/trip. |
| `GET` | `/api/owner/finance/expenses` | Lists operating expenses filtered by category, date, or lorry. |
| `POST` | `/api/owner/finance/expenses` | Records operating expense (Diesel, Toll, Repairs, Tyres, Servicing, Other). |
| `GET` | `/api/owner/finance/maintenance` | Lists workshop maintenance records and upcoming service milestones. |
| `POST` | `/api/owner/finance/maintenance` | Logs vehicle workshop maintenance and sets next service due odometer/date. |
| `GET` | `/api/owner/reports/monthly` | Computes monthly P&L summary, lorry-wise breakdown, and expense percentages. |
| `GET` | `/api/owner/reports/monthly/pdf` | Generates a binary PDF document (`application/pdf`) for download and sharing. |

---

## 3. Request / Response Contract Payloads

### 3.1 Trip Creation Contract (`POST /api/owner/trips`)
```json
{
  "lorryId": 2,
  "originLocation": "Nellore Industrial Park",
  "destinationLocation": "Sanathnagar Goods Yard, Hyderabad",
  "destinationLat": 17.4589210,
  "destinationLng": 78.4312540,
  "tripAmount": 28500.00,
  "primaryDriverId": 5,
  "coDriverIds": [8],
  "scheduledStartDate": "2026-10-25T06:00:00Z",
  "cargoNotes": "20 Tons Steel Coils. Handle with care. Delivery before 6 PM."
}
```

### 3.2 Driver Dashboard Contract (`GET /api/driver/trips/dashboard`)
*Notice the complete absence of `tripAmount` or financial variables.*
```json
{
  "driverName": "Ramesh Kumar",
  "activeTrip": {
    "tripId": 14,
    "tripCode": "TRP-2026-0014",
    "status": "IN_PROGRESS",
    "lorry": {
      "registrationNumber": "AP 26 XX 1234",
      "model": "Ashok Leyland 2820 HG"
    },
    "originLocation": "Nellore Industrial Park",
    "destinationLocation": "Sanathnagar Goods Yard, Hyderabad",
    "destinationLat": 17.4589210,
    "destinationLng": 78.4312540,
    "actualStartTime": "2026-10-25T06:15:00Z",
    "coDrivers": ["Suresh Babu"]
  },
  "upcomingTripsCount": 1,
  "completedTripsTotal": 38
}
```

### 3.3 Destination Reached Check Contract (`POST /api/driver/trips/{id}/reached-destination`)
**Request:**
```json
{
  "latitude": 17.4591100,
  "longitude": 78.4310200,
  "accuracyMeters": 18.5,
  "recordedAt": "2026-10-25T14:32:10Z"
}
```

**Response (Verified Success $\le 500$m):**
```json
{
  "success": true,
  "verifiedByGps": true,
  "distanceMeters": 42.8,
  "status": "AWAITING_OWNER_CONFIRMATION",
  "message": "Destination arrival verified! Request submitted to Fleet Owner for confirmation."
}
```

**Response (Outside Geofence $> 500$m - HTTP 422):**
```json
{
  "timestamp": "2026-10-25T14:32:12Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Current location is 1,420 metres from the scheduled destination. Verification requires proximity within 500 metres.",
  "path": "/api/driver/trips/14/reached-destination",
  "distanceMeters": 1420.0,
  "canSubmitManualReview": true
}
```

### 3.4 Owner Completion Confirmation Contract (`POST /api/owner/trips/{id}/confirm`)
```json
{
  "action": "APPROVE",
  "postTripLorryStatus": "AVAILABLE",
  "ownerNotes": "Consignee confirmed cargo unloading completed without issues."
}
```
*(If the owner selects `postTripLorryStatus: "MAINTENANCE"`, the lorry status immediately updates to `MAINTENANCE` and becomes ineligible for future dispatches until cleared).*
