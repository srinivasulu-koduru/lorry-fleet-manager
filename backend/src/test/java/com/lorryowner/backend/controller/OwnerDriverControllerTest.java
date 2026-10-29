package com.lorryowner.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorryowner.backend.dto.auth.LoginRequest;
import com.lorryowner.backend.dto.driver.AssignLorryRequest;
import com.lorryowner.backend.dto.driver.CreateDriverRequest;
import com.lorryowner.backend.dto.driver.DriverStatusRequest;
import com.lorryowner.backend.dto.driver.UpdateDriverRequest;
import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.OperationalStatus;
import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.entity.Trip;
import com.lorryowner.backend.entity.TripStatus;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.TripRepository;
import com.lorryowner.backend.repository.UserRepository;
import com.lorryowner.backend.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OwnerDriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LorryRepository lorryRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private com.lorryowner.backend.repository.MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User owner;
    private User otherOwner;
    private String ownerToken;
    private String otherOwnerToken;
    private String driverToken;
    private User driver;
    private Lorry fleetLorry;
    private Lorry otherOwnerLorry;

    @BeforeEach
    void setUp() {
        cleanupDatabase();

        // 1. Primary Owner
        owner = new User(
                "fleet_boss",
                passwordEncoder.encode("BossPassword123!"),
                Role.OWNER,
                "Fleet Boss",
                "9876543210",
                "boss@example.com"
        );
        owner = userRepository.save(owner);
        ownerToken = jwtService.generateToken(owner);

        // 2. Another Owner (for IDOR tests)
        otherOwner = new User(
                "rival_boss",
                passwordEncoder.encode("RivalPassword123!"),
                Role.OWNER,
                "Rival Boss",
                "9876543222",
                "rival@example.com"
        );
        otherOwner = userRepository.save(otherOwner);
        otherOwnerToken = jwtService.generateToken(otherOwner);

        // 3. Lorries
        fleetLorry = new Lorry("AP 31 TC 1234", "Ashok Leyland 2820 HG", new BigDecimal("20.00"), OperationalStatus.AVAILABLE);
        fleetLorry.setOwner(owner);
        fleetLorry = lorryRepository.save(fleetLorry);

        otherOwnerLorry = new Lorry("TS 09 UA 9999", "Tata Signa 4825.TK", new BigDecimal("35.00"), OperationalStatus.AVAILABLE);
        otherOwnerLorry.setOwner(otherOwner);
        otherOwnerLorry = lorryRepository.save(otherOwnerLorry);

        // 4. Driver belonging to primary Owner
        driver = new User(
                "driver_john",
                passwordEncoder.encode("DriverPass123!"),
                Role.DRIVER,
                "John Doe",
                "9876543211",
                "DL-IND-JOHN"
        );
        driver.setOwner(owner);
        driver = userRepository.save(driver);
        driverToken = jwtService.generateToken(driver);
    }

    @Test
    @DisplayName("Owner can create a new permanent Driver account with hashed password")
    void createDriver_asOwner_shouldSucceed() throws Exception {
        CreateDriverRequest request = new CreateDriverRequest(
                "driver_rajesh",
                "DriverRajeshPass123!",
                "Rajesh Sharma",
                "9988776655",
                "DL-IND-001"
        );

        mockMvc.perform(post("/api/owner/drivers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("driver_rajesh"))
                .andExpect(jsonPath("$.fullName").value("Rajesh Sharma"))
                .andExpect(jsonPath("$.phoneNumber").value("9988776655"))
                .andExpect(jsonPath("$.isActive").value(true));

        // Verify password is encrypted in database
        User savedDriver = userRepository.findByUsername("driver_rajesh").orElseThrow();
        assertThat(passwordEncoder.matches("DriverRajeshPass123!", savedDriver.getPasswordHash())).isTrue();
        assertThat(savedDriver.getPasswordHash()).doesNotContain("DriverRajeshPass123!");
    }

    @Test
    @DisplayName("Owner can create driver with lorry assignment by registration")
    void createDriver_withLorryAssignment_shouldSucceed() throws Exception {
        CreateDriverRequest request = new CreateDriverRequest(
                "driver_assigned",
                "DriverPass123!",
                "Assigned Driver",
                "9123456780",
                "DL-ASSIGN-01",
                "AP 31 TC 1234",
                null
        );

        mockMvc.perform(post("/api/owner/drivers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignedLorryRegistration").value("AP 31 TC 1234"));

        User saved = userRepository.findByUsername("driver_assigned").orElseThrow();
        assertThat(saved.getAssignedLorry()).isNotNull();
        assertThat(saved.getAssignedLorry().getRegistrationNumber()).isEqualTo("AP 31 TC 1234");
    }

    @Test
    @DisplayName("Owner can list their registered drivers")
    void getAllDrivers_asOwner_shouldReturnDriverList() throws Exception {
        mockMvc.perform(get("/api/owner/drivers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("driver_john"))
                .andExpect(jsonPath("$[0].fullName").value("John Doe"));
    }

    @Test
    @DisplayName("Owner can get single driver details by ID")
    void getDriverById_asOwner_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/owner/drivers/" + driver.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(driver.getId()))
                .andExpect(jsonPath("$.username").value("driver_john"))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    @DisplayName("Getting non-existent driver returns 404 Not Found")
    void getDriverById_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/owner/drivers/99999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Owner cannot view another owner's driver (IDOR protection returns 404)")
    void getDriverById_otherOwnerDriver_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/owner/drivers/" + driver.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Owner can update driver details")
    void updateDriver_asOwner_shouldSucceed() throws Exception {
        UpdateDriverRequest request = new UpdateDriverRequest(
                "John Updated Doe",
                "9876549999",
                "DL-UPDATED-01"
        );

        mockMvc.perform(put("/api/owner/drivers/" + driver.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Updated Doe"))
                .andExpect(jsonPath("$.phoneNumber").value("9876549999"))
                .andExpect(jsonPath("$.licenseNumber").value("DL-UPDATED-01"));
    }

    @Test
    @DisplayName("Owner can assign lorry to driver")
    void assignLorry_asOwner_shouldSucceed() throws Exception {
        AssignLorryRequest request = new AssignLorryRequest(fleetLorry.getId(), null);

        mockMvc.perform(put("/api/owner/drivers/" + driver.getId() + "/assign-lorry")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedLorryRegistration").value("AP 31 TC 1234"));

        User updated = userRepository.findById(driver.getId()).orElseThrow();
        assertThat(updated.getAssignedLorry()).isNotNull();
        assertThat(updated.getAssignedLorry().getId()).isEqualTo(fleetLorry.getId());
    }

    @Test
    @DisplayName("Owner can unassign lorry from driver")
    void unassignLorry_asOwner_shouldSucceed() throws Exception {
        driver.setAssignedLorry(fleetLorry);
        userRepository.save(driver);

        AssignLorryRequest request = new AssignLorryRequest(null, null);

        mockMvc.perform(put("/api/owner/drivers/" + driver.getId() + "/assign-lorry")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedLorryRegistration").isEmpty());

        User updated = userRepository.findById(driver.getId()).orElseThrow();
        assertThat(updated.getAssignedLorry()).isNull();
    }

    @Test
    @DisplayName("Owner cannot assign another owner's lorry (returns 403 Forbidden)")
    void assignLorry_otherOwnerLorry_shouldReturn403() throws Exception {
        AssignLorryRequest request = new AssignLorryRequest(otherOwnerLorry.getId(), null);

        mockMvc.perform(put("/api/owner/drivers/" + driver.getId() + "/assign-lorry")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Owner can toggle driver status and deactivated driver cannot log in")
    void toggleDriverStatus_deactivatedCannotLogin() throws Exception {
        DriverStatusRequest request = new DriverStatusRequest(false);

        // 1. Deactivate driver
        mockMvc.perform(patch("/api/owner/drivers/" + driver.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // 2. Attempt login as deactivated driver -> 401 Unauthorized
        LoginRequest loginReq = new LoginRequest("driver_john", "DriverPass123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());

        // 3. Reactivate driver
        DriverStatusRequest reactivateReq = new DriverStatusRequest(true);
        mockMvc.perform(put("/api/owner/drivers/" + driver.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reactivateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));

        // 4. Now driver can log in again -> 200 OK
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Owner can safely delete/deactivate driver")
    void deleteDriver_asOwner_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/owner/drivers/" + driver.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(driver.getId())).isEmpty();
    }

    @Test
    @DisplayName("Driver role cannot create another driver account (returns 403 Forbidden)")
    void createDriver_asDriver_shouldReturn403() throws Exception {
        CreateDriverRequest request = new CreateDriverRequest(
                "driver_new",
                "NewDriverPass123!",
                "New Guy",
                "9988776600",
                null
        );

        mockMvc.perform(post("/api/owner/drivers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Driver role cannot view driver management list (returns 403 Forbidden)")
    void getAllDrivers_asDriver_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/owner/drivers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Unauthenticated request to driver management returns 401 Unauthorized")
    void driverManagement_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/owner/drivers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Creating driver with duplicate username returns 409 Conflict")
    void createDriver_withDuplicateUsername_shouldReturn409() throws Exception {
        CreateDriverRequest request = new CreateDriverRequest(
                "driver_john",
                "AnotherPassword123!",
                "Duplicate John",
                "9876543299",
                null
        );

        mockMvc.perform(post("/api/owner/drivers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username 'driver_john' is already in use."));
    }

    @Test
    @DisplayName("Creating driver with weak password returns 400 Bad Request")
    void createDriver_withWeakPassword_shouldReturn400() throws Exception {
        CreateDriverRequest request = new CreateDriverRequest(
                "driver_weak",
                "short",
                "Weak Pass Driver",
                "9876543299",
                null
        );

        mockMvc.perform(post("/api/owner/drivers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Owner can search drivers by full name and username case-insensitively")
    void getAllDrivers_searchByNameAndUsername_shouldFilterCorrectly() throws Exception {
        // Create second driver
        User driver2 = new User(
                "driver_venkat",
                passwordEncoder.encode("Password123!"),
                Role.DRIVER,
                "Venkatesh Rao",
                "9876500000",
                "DL-AP-01"
        );
        driver2.setOwner(owner);
        userRepository.save(driver2);

        // Search by name "venkat"
        mockMvc.perform(get("/api/owner/drivers?search=venkat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("driver_venkat"));

        // Search by username uppercase "JOHN"
        mockMvc.perform(get("/api/owner/drivers?search=JOHN")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName").value("John Doe"));
    }

    @Test
    @DisplayName("Owner can filter drivers by status and lorry assignment")
    void getAllDrivers_filterByStatusAndAssignment_shouldReturnMatching() throws Exception {
        // Driver 1 (driver) is assigned fleetLorry
        driver.setAssignedLorry(fleetLorry);
        userRepository.save(driver);

        // Create Driver 2: Inactive, no lorry
        User driver2 = new User(
                "driver_inactive",
                passwordEncoder.encode("Password123!"),
                Role.DRIVER,
                "Inactive Driver",
                null,
                null
        );
        driver2.setOwner(owner);
        driver2.setActive(false);
        userRepository.save(driver2);

        // Filter ACTIVE
        mockMvc.perform(get("/api/owner/drivers?status=ACTIVE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("driver_john"));

        // Filter INACTIVE
        mockMvc.perform(get("/api/owner/drivers?status=INACTIVE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("driver_inactive"));

        // Filter ASSIGNED
        mockMvc.perform(get("/api/owner/drivers?assigned=ASSIGNED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("driver_john"));

        // Filter UNASSIGNED
        mockMvc.perform(get("/api/owner/drivers?assigned=UNASSIGNED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("driver_inactive"));
    }

    @Test
    @DisplayName("Owner can fetch driver trip history ordered newest first")
    void getDriverTrips_asOwner_shouldReturnChronologicalTrips() throws Exception {
        // Seed trips for driver
        Trip trip1 = new Trip(
                "TRIP-101",
                driver,
                fleetLorry,
                owner,
                "Visakhapatnam",
                "Vijayawada",
                TripStatus.COMPLETED,
                LocalDate.of(2026, 10, 15),
                Instant.parse("2026-10-15T08:00:00Z"),
                Instant.parse("2026-10-15T15:00:00Z"),
                new BigDecimal("350.00"),
                "Delivered successfully"
        );
        Trip trip2 = new Trip(
                "TRIP-102",
                driver,
                fleetLorry,
                owner,
                "Vijayawada",
                "Hyderabad",
                TripStatus.COMPLETED,
                LocalDate.of(2026, 10, 20),
                Instant.parse("2026-10-20T08:00:00Z"),
                Instant.parse("2026-10-20T14:30:00Z"),
                new BigDecimal("275.50"),
                "Goods received"
        );
        tripRepository.save(trip1);
        tripRepository.save(trip2);

        mockMvc.perform(get("/api/owner/drivers/" + driver.getId() + "/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.trips[0].tripReference").value("TRIP-102")) // Newest first
                .andExpect(jsonPath("$.trips[0].lorryRegistration").value("AP 31 TC 1234"))
                .andExpect(jsonPath("$.trips[1].tripReference").value("TRIP-101"));
    }

    @Test
    @DisplayName("Owner can filter driver trips by status, date range, and location")
    void getDriverTrips_withFilters_shouldReturnMatchingTrips() throws Exception {
        Trip trip1 = new Trip(
                "TRIP-201",
                driver,
                fleetLorry,
                owner,
                "Vizag",
                "Guntur",
                TripStatus.COMPLETED,
                LocalDate.of(2026, 10, 10),
                Instant.parse("2026-10-10T08:00:00Z"),
                Instant.parse("2026-10-10T14:00:00Z"),
                new BigDecimal("380.00"),
                null
        );
        Trip trip2 = new Trip(
                "TRIP-202",
                driver,
                fleetLorry,
                owner,
                "Vizag",
                "Tirupati",
                TripStatus.IN_PROGRESS,
                LocalDate.of(2026, 10, 26),
                Instant.parse("2026-10-26T09:00:00Z"),
                null,
                null,
                "On route"
        );
        tripRepository.save(trip1);
        tripRepository.save(trip2);

        // Filter status COMPLETED
        mockMvc.perform(get("/api/owner/drivers/" + driver.getId() + "/trips?status=COMPLETED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.trips[0].tripReference").value("TRIP-201"));

        // Filter date range
        mockMvc.perform(get("/api/owner/drivers/" + driver.getId() + "/trips?startDate=2026-10-25&endDate=2026-10-27")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.trips[0].tripReference").value("TRIP-202"));
    }

    @Test
    @DisplayName("Historical trips retain original lorry registration even if driver assigned lorry changes")
    void tripHistory_retainsOriginalLorryWhenAssignmentChanges() throws Exception {
        // Driver had trip with fleetLorry (AP 31 TC 1234)
        Trip oldTrip = new Trip(
                "TRIP-HISTORICAL",
                driver,
                fleetLorry,
                owner,
                "Chennai",
                "Bangalore",
                TripStatus.COMPLETED,
                LocalDate.of(2026, 9, 1),
                Instant.parse("2026-09-01T06:00:00Z"),
                Instant.parse("2026-09-01T15:00:00Z"),
                new BigDecimal("340.00"),
                null
        );
        tripRepository.save(oldTrip);

        // Reassign driver to second lorry
        Lorry secondLorry = new Lorry("AP 26 TD 5678", "Ashok Leyland 3520", new BigDecimal("25.00"), OperationalStatus.AVAILABLE);
        secondLorry.setOwner(owner);
        secondLorry = lorryRepository.save(secondLorry);

        AssignLorryRequest assignReq = new AssignLorryRequest(secondLorry.getId(), null);
        mockMvc.perform(put("/api/owner/drivers/" + driver.getId() + "/assign-lorry")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedLorryRegistration").value("AP 26 TD 5678"));

        // Verify historical trip STILL displays the original lorry AP 31 TC 1234
        mockMvc.perform(get("/api/owner/drivers/" + driver.getId() + "/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips[0].lorryRegistration").value("AP 31 TC 1234"));
    }

    @Test
    @DisplayName("Driver performance metrics calculate accurate counts and distance")
    void getDriverPerformance_shouldReturnAccurateCalculations() throws Exception {
        Trip trip1 = new Trip(
                "TRIP-PERF-1",
                driver,
                fleetLorry,
                owner,
                "Origin A",
                "Dest A",
                TripStatus.COMPLETED,
                LocalDate.of(2026, 10, 10),
                Instant.parse("2026-10-10T08:00:00Z"),
                Instant.parse("2026-10-10T14:00:00Z"),
                new BigDecimal("300.50"),
                null
        );
        Trip trip2 = new Trip(
                "TRIP-PERF-2",
                driver,
                fleetLorry,
                owner,
                "Origin B",
                "Dest B",
                TripStatus.COMPLETED,
                LocalDate.of(2026, 10, 12),
                Instant.parse("2026-10-12T08:00:00Z"),
                Instant.parse("2026-10-12T16:00:00Z"),
                new BigDecimal("200.50"),
                null
        );
        Trip trip3 = new Trip(
                "TRIP-PERF-3",
                driver,
                fleetLorry,
                owner,
                "Origin C",
                "Dest C",
                TripStatus.IN_PROGRESS,
                LocalDate.of(2026, 10, 15),
                Instant.parse("2026-10-15T09:00:00Z"),
                null,
                null,
                null
        );
        tripRepository.save(trip1);
        tripRepository.save(trip2);
        tripRepository.save(trip3);

        mockMvc.perform(get("/api/owner/drivers/" + driver.getId() + "/performance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTrips").value(3))
                .andExpect(jsonPath("$.completedTrips").value(2))
                .andExpect(jsonPath("$.inProgressTrips").value(1))
                .andExpect(jsonPath("$.cancelledTrips").value(0))
                .andExpect(jsonPath("$.totalRecordedDistanceKm").value(501.0))
                .andExpect(jsonPath("$.distinctLorriesUsed").value(1))
                .andExpect(jsonPath("$.mostRecentTripReference").value("TRIP-PERF-2"));
    }

    @Test
    @DisplayName("Owner cannot view trips or performance of driver belonging to another owner (404/IDOR prevention)")
    void crossOwner_tripAccess_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/owner/drivers/" + driver.getId() + "/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/owner/drivers/" + driver.getId() + "/performance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Driver role cannot access trip history or performance endpoints (403 Forbidden)")
    void driverRole_tripHistoryAccess_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/owner/drivers/" + driver.getId() + "/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/owner/drivers/" + driver.getId() + "/performance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    @AfterEach
    void tearDown() {
        cleanupDatabase();
    }

    private void cleanupDatabase() {
        maintenanceRecordRepository.deleteAll();
        tripRepository.deleteAll();
        userRepository.findAll().forEach(u -> {
            u.setAssignedLorry(null);
            u.setOwner(null);
            userRepository.save(u);
        });
        lorryRepository.deleteAll();
        userRepository.deleteAll();
    }
}
