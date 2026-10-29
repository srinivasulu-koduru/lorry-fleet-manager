package com.lorryowner.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorryowner.backend.dto.lorry.CreateLorryRequest;
import com.lorryowner.backend.entity.*;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.MaintenanceRecordRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OwnerLorryControllerTest {

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
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User owner;
    private User otherOwner;
    private User driver;
    private String ownerToken;
    private String otherOwnerToken;
    private String driverToken;

    private Lorry runningLorry;
    private Lorry stoppedLorry;
    private Lorry otherOwnerLorry;

    @BeforeEach
    void setUp() {
        cleanupDatabase();

        // 1. Create primary owner
        owner = new User(
                "fleet_owner_lorry_test",
                passwordEncoder.encode("SecurePass123!"),
                Role.OWNER,
                "Fleet Owner Primary",
                "9876543210",
                "owner1@fleet.test"
        );
        owner = userRepository.save(owner);
        ownerToken = jwtService.generateToken(owner);

        // 2. Create another owner for IDOR tests
        otherOwner = new User(
                "other_owner_lorry_test",
                passwordEncoder.encode("SecurePass123!"),
                Role.OWNER,
                "Rival Owner",
                "9876543211",
                "owner2@fleet.test"
        );
        otherOwner = userRepository.save(otherOwner);
        otherOwnerToken = jwtService.generateToken(otherOwner);

        // 3. Create lorries
        runningLorry = new Lorry("AP 31 TC 1234", "Tata Signa 4923", new BigDecimal("25.00"), OperationalStatus.ON_TRIP);
        runningLorry.setOwner(owner);
        runningLorry = lorryRepository.save(runningLorry);

        stoppedLorry = new Lorry("TS 09 UA 5678", "Ashok Leyland 2820", new BigDecimal("20.00"), OperationalStatus.AVAILABLE);
        stoppedLorry.setOwner(owner);
        stoppedLorry = lorryRepository.save(stoppedLorry);

        otherOwnerLorry = new Lorry("KA 05 MB 9999", "Volvo FH 500", new BigDecimal("30.00"), OperationalStatus.AVAILABLE);
        otherOwnerLorry.setOwner(otherOwner);
        otherOwnerLorry = lorryRepository.save(otherOwnerLorry);

        // 4. Create driver assigned to runningLorry
        driver = new User(
                "driver_ramesh_test",
                passwordEncoder.encode("DriverPass123!"),
                Role.DRIVER,
                "Ramesh Kumar",
                "9876543210",
                "DL-TEST-001"
        );
        driver.setOwner(owner);
        driver.setAssignedLorry(runningLorry);
        driver = userRepository.save(driver);
        driverToken = jwtService.generateToken(driver);

        // 5. Create trips for runningLorry
        Trip inProgressTrip = new Trip(
                "TRP-TEST-001",
                driver,
                runningLorry,
                owner,
                "Visakhapatnam",
                "Vijayawada",
                TripStatus.IN_PROGRESS,
                LocalDate.now(),
                Instant.now().minusSeconds(7200),
                null,
                new BigDecimal("350.00"),
                "High priority cargo"
        );
        inProgressTrip.setPlannedDeliveryTime(Instant.now().plusSeconds(14400)); // +4 hours
        tripRepository.save(inProgressTrip);

        Trip completedTrip = new Trip(
                "TRP-TEST-002",
                driver,
                runningLorry,
                owner,
                "Vijayawada",
                "Hyderabad",
                TripStatus.COMPLETED,
                LocalDate.now().minusDays(2),
                Instant.now().minusSeconds(172800),
                Instant.now().minusSeconds(150000),
                new BigDecimal("275.50"),
                "Delivered on time"
        );
        tripRepository.save(completedTrip);

        // 6. Create maintenance records for runningLorry
        MaintenanceRecord record1 = new MaintenanceRecord(
                runningLorry,
                owner,
                LocalDate.now().minusDays(30),
                "Regular Service",
                "Engine oil change, filter replacement",
                new BigDecimal("8500.00"),
                "SRS Auto Center",
                45230,
                LocalDate.now().plusDays(60)
        );
        maintenanceRecordRepository.save(record1);

        MaintenanceRecord record2 = new MaintenanceRecord(
                runningLorry,
                owner,
                LocalDate.now().minusDays(90),
                "Brake Service",
                "Brake pad replacement",
                new BigDecimal("12000.00"),
                "Sai Motors",
                38760,
                LocalDate.now().minusDays(30)
        );
        maintenanceRecordRepository.save(record2);
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

    @Test
    @DisplayName("GET /api/owner/lorries returns all owner's lorries")
    void getAllLorries_asOwner_shouldReturnOnlyOwnerLorries() throws Exception {
        mockMvc.perform(get("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.registrationNumber == 'AP 31 TC 1234')].running").value(true))
                .andExpect(jsonPath("$[?(@.registrationNumber == 'TS 09 UA 5678')].running").value(false));
    }

    @Test
    @DisplayName("GET /api/owner/lorries?filter=RUNNING returns only running lorries")
    void getAllLorries_filterRunning_shouldReturnRunningOnly() throws Exception {
        mockMvc.perform(get("/api/owner/lorries?filter=RUNNING")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].registrationNumber").value("AP 31 TC 1234"))
                .andExpect(jsonPath("$[0].running").value(true))
                .andExpect(jsonPath("$[0].activeTripDestination").value("Vijayawada"))
                .andExpect(jsonPath("$[0].assignedDriverName").value("Ramesh Kumar"));
    }

    @Test
    @DisplayName("GET /api/owner/lorries?filter=NOT_RUNNING returns only stopped lorries")
    void getAllLorries_filterNotRunning_shouldReturnNotRunningOnly() throws Exception {
        mockMvc.perform(get("/api/owner/lorries?filter=NOT_RUNNING")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].registrationNumber").value("TS 09 UA 5678"))
                .andExpect(jsonPath("$[0].running").value(false));
    }

    @Test
    @DisplayName("GET /api/owner/lorries?search=Signa filters by model or reg number")
    void getAllLorries_search_shouldReturnMatching() throws Exception {
        mockMvc.perform(get("/api/owner/lorries?search=Signa")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].registrationNumber").value("AP 31 TC 1234"));
    }

    @Test
    @DisplayName("GET /api/owner/lorries/summary returns accurate fleet statistics")
    void getFleetSummary_shouldReturnCalculatedStatistics() throws Exception {
        mockMvc.perform(get("/api/owner/lorries/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLorries").value(2))
                .andExpect(jsonPath("$.runningLorries").value(1))
                .andExpect(jsonPath("$.notRunningLorries").value(1))
                .andExpect(jsonPath("$.runningPercentage").value(50.0))
                .andExpect(jsonPath("$.notRunningPercentage").value(50.0))
                .andExpect(jsonPath("$.totalTrips").value(2))
                .andExpect(jsonPath("$.totalDistanceKm").value(275.50))
                .andExpect(jsonPath("$.totalMaintenanceCost").value(20500.00));
    }

    @Test
    @DisplayName("GET /api/owner/lorries/{id} returns full lorry details")
    void getLorryDetails_shouldReturnCompleteDetails() throws Exception {
        mockMvc.perform(get("/api/owner/lorries/" + runningLorry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationNumber").value("AP 31 TC 1234"))
                .andExpect(jsonPath("$.makeModel").value("Tata Signa 4923"))
                .andExpect(jsonPath("$.running").value(true))
                .andExpect(jsonPath("$.assignedDriverName").value("Ramesh Kumar"))
                .andExpect(jsonPath("$.activeTripReference").value("TRP-TEST-001"))
                .andExpect(jsonPath("$.activeTripOrigin").value("Visakhapatnam"))
                .andExpect(jsonPath("$.activeTripDestination").value("Vijayawada"))
                .andExpect(jsonPath("$.activeTripEtaFormatted", containsString("ETA:")))
                .andExpect(jsonPath("$.totalTrips").value(2))
                .andExpect(jsonPath("$.completedTrips").value(1))
                .andExpect(jsonPath("$.inProgressTrips").value(1))
                .andExpect(jsonPath("$.totalMaintenanceCost").value(20500.00));
    }

    @Test
    @DisplayName("GET /api/owner/lorries/{id}/trips returns historical trips for that specific lorry")
    void getLorryTrips_shouldReturnLorryTripsNewestFirst() throws Exception {
        mockMvc.perform(get("/api/owner/lorries/" + runningLorry.getId() + "/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.trips[0].tripReference").value("TRP-TEST-001"))
                .andExpect(jsonPath("$.trips[1].tripReference").value("TRP-TEST-002"));
    }

    @Test
    @DisplayName("GET /api/owner/lorries/{id}/trips?status=COMPLETED filters by trip status")
    void getLorryTrips_filterStatus_shouldReturnFiltered() throws Exception {
        mockMvc.perform(get("/api/owner/lorries/" + runningLorry.getId() + "/trips?status=COMPLETED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.trips[0].tripReference").value("TRP-TEST-002"));
    }

    @Test
    @DisplayName("GET /api/owner/lorries/{id}/maintenance returns maintenance records and total cost")
    void getLorryMaintenance_shouldReturnRecordsAndTotalCost() throws Exception {
        mockMvc.perform(get("/api/owner/lorries/" + runningLorry.getId() + "/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationNumber").value("AP 31 TC 1234"))
                .andExpect(jsonPath("$.totalMaintenanceCost").value(20500.00))
                .andExpect(jsonPath("$.records", hasSize(2)))
                .andExpect(jsonPath("$.records[0].serviceType").value("Regular Service"))
                .andExpect(jsonPath("$.records[0].cost").value(8500.00))
                .andExpect(jsonPath("$.records[1].serviceType").value("Brake Service"))
                .andExpect(jsonPath("$.records[1].cost").value(12000.00));
    }

    @Test
    @DisplayName("Owner cannot view details, trips, or maintenance of another owner's lorry (404 Not Found)")
    void crossOwnerAccess_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/owner/lorries/" + otherOwnerLorry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/owner/lorries/" + otherOwnerLorry.getId() + "/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/owner/lorries/" + otherOwnerLorry.getId() + "/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Driver role is forbidden from accessing owner lorry endpoints (403 Forbidden)")
    void driverRole_lorryAccess_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/owner/lorries/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/owner/lorries creates lorry successfully without driver")
    void createLorry_success_withoutDriver() throws Exception {
        CreateLorryRequest request = new CreateLorryRequest(
                "AP 09 CD 5555",
                "BharatBenz 3528C",
                new BigDecimal("28.00"),
                "AVAILABLE",
                "IALERT-5555",
                "Heavy tipper unit",
                null
        );

        mockMvc.perform(post("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.registrationNumber").value("AP 09 CD 5555"))
                .andExpect(jsonPath("$.makeModel").value("BharatBenz 3528C"))
                .andExpect(jsonPath("$.capacityTons").value(28.00))
                .andExpect(jsonPath("$.operationalStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.running").value(false))
                .andExpect(jsonPath("$.notes").value("Heavy tipper unit"));
    }

    @Test
    @DisplayName("POST /api/owner/lorries creates lorry with driver assignment successfully")
    void createLorry_success_withDriverAssignment() throws Exception {
        User unassignedDriver = new User(
                "driver_suresh_lorry_test",
                passwordEncoder.encode("DriverPass123!"),
                Role.DRIVER,
                "Suresh Patel",
                "9876543299",
                "DL-TEST-099"
        );
        unassignedDriver.setOwner(owner);
        unassignedDriver = userRepository.save(unassignedDriver);

        CreateLorryRequest request = new CreateLorryRequest(
                "MH 12 EF 7777",
                "Eicher Pro 3019",
                new BigDecimal("19.00"),
                null,
                null,
                null,
                unassignedDriver.getId()
        );

        mockMvc.perform(post("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationNumber").value("MH 12 EF 7777"))
                .andExpect(jsonPath("$.assignedDriverId").value(unassignedDriver.getId()))
                .andExpect(jsonPath("$.assignedDriverName").value("Suresh Patel"))
                .andExpect(jsonPath("$.running").value(false));

        // Verify driver record updated in DB
        User updatedDriver = userRepository.findById(unassignedDriver.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotNull(updatedDriver.getAssignedLorry());
        org.junit.jupiter.api.Assertions.assertEquals("MH 12 EF 7777", updatedDriver.getAssignedLorry().getRegistrationNumber());
    }

    @Test
    @DisplayName("POST /api/owner/lorries rejects duplicate registration number (case-insensitive)")
    void createLorry_duplicateRegistration_shouldReturnBadRequest() throws Exception {
        // runningLorry has registration "AP 31 TC 1234"
        CreateLorryRequest request = new CreateLorryRequest(
                "ap 31 tc 1234", // lower case variant
                "Tata Prima 407",
                new BigDecimal("16.00")
        );

        mockMvc.perform(post("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/owner/lorries rejects invalid or missing required fields")
    void createLorry_missingFields_shouldReturnBadRequest() throws Exception {
        // Blank registration
        CreateLorryRequest req1 = new CreateLorryRequest("", "Tata Prima", new BigDecimal("16.0"));
        mockMvc.perform(post("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isBadRequest());

        // Blank model
        CreateLorryRequest req2 = new CreateLorryRequest("KA 01 BB 1234", "", new BigDecimal("16.0"));
        mockMvc.perform(post("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest());

        // Zero / negative capacity
        CreateLorryRequest req3 = new CreateLorryRequest("KA 01 BB 1234", "Tata Prima", new BigDecimal("-5.0"));
        mockMvc.perform(post("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/owner/lorries rejects assigning driver belonging to another owner (403 Forbidden)")
    void createLorry_crossOwnerDriverAssignment_shouldReturnForbidden() throws Exception {
        User otherOwnerDriver = new User(
                "driver_rival_test",
                passwordEncoder.encode("DriverPass123!"),
                Role.DRIVER,
                "Rival Driver",
                "9876543298",
                "DL-TEST-998"
        );
        otherOwnerDriver.setOwner(otherOwner);
        otherOwnerDriver = userRepository.save(otherOwnerDriver);

        CreateLorryRequest request = new CreateLorryRequest(
                "DL 01 AB 9988",
                "Tata Prima",
                new BigDecimal("16.00"),
                null,
                null,
                null,
                otherOwnerDriver.getId()
        );

        mockMvc.perform(post("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/owner/lorries rejects assigning inactive driver (400 Bad Request)")
    void createLorry_inactiveDriverAssignment_shouldReturnBadRequest() throws Exception {
        User inactiveDriver = new User(
                "driver_inactive_lorry_test",
                passwordEncoder.encode("DriverPass123!"),
                Role.DRIVER,
                "Inactive Driver",
                "9876543297",
                "DL-TEST-997"
        );
        inactiveDriver.setOwner(owner);
        inactiveDriver.setActive(false);
        inactiveDriver = userRepository.save(inactiveDriver);

        CreateLorryRequest request = new CreateLorryRequest(
                "DL 02 CD 8877",
                "Tata Prima",
                new BigDecimal("16.00"),
                null,
                null,
                null,
                inactiveDriver.getId()
        );

        mockMvc.perform(post("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/owner/lorries rejects non-owner roles (403 Forbidden)")
    void createLorry_driverRole_shouldReturnForbidden() throws Exception {
        CreateLorryRequest request = new CreateLorryRequest(
                "DL 03 EF 7766",
                "Tata Prima",
                new BigDecimal("16.00")
        );

        mockMvc.perform(post("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
