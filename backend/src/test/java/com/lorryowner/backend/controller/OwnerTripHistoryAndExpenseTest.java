package com.lorryowner.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorryowner.backend.dto.maintenance.CreateMaintenanceRecordRequest;
import com.lorryowner.backend.dto.trip.CreateTripExpenseRequest;
import com.lorryowner.backend.entity.*;
import com.lorryowner.backend.repository.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OwnerTripHistoryAndExpenseTest {

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
    private FinanceTransactionRepository financeTransactionRepository;

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

    private Lorry ownerLorry;
    private Lorry otherOwnerLorry;
    private Trip activeTrip;

    @BeforeEach
    void setUp() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        cleanupDatabase();

        // 1. Primary Owner
        owner = new User(
                "owner_hist_test",
                passwordEncoder.encode("OwnerPass123!"),
                Role.OWNER,
                "Srinivasulu Fleet Owner",
                "9876543210",
                "owner_hist@bhp.test"
        );
        owner = userRepository.save(owner);
        ownerToken = jwtService.generateToken(owner);

        // 2. Other Owner (for cross-owner IDOR test)
        otherOwner = new User(
                "other_owner_hist_test",
                passwordEncoder.encode("OtherPass123!"),
                Role.OWNER,
                "Other Fleet Owner",
                "9876543211",
                "other_hist@bhp.test"
        );
        otherOwner = userRepository.save(otherOwner);
        otherOwnerToken = jwtService.generateToken(otherOwner);

        // 3. Driver
        driver = new User(
                "driver_hist_test",
                passwordEncoder.encode("DriverPass123!"),
                Role.DRIVER,
                "Ramesh Kumar",
                "9876543212",
                "DL-TEST-HIST"
        );
        driver.setOwner(owner);
        driver.setActive(true);
        driver = userRepository.save(driver);
        driverToken = jwtService.generateToken(driver);

        // 4. Lorries
        ownerLorry = new Lorry("AP 39 XX 1234", "Tata Prima 4928", new BigDecimal("28.00"), OperationalStatus.ON_TRIP);
        ownerLorry.setOwner(owner);
        ownerLorry = lorryRepository.save(ownerLorry);

        otherOwnerLorry = new Lorry("TS 09 UA 9999", "Volvo FH 500", new BigDecimal("32.00"), OperationalStatus.AVAILABLE);
        otherOwnerLorry.setOwner(otherOwner);
        otherOwnerLorry = lorryRepository.save(otherOwnerLorry);

        // 5. Trip for owner (Nellore -> Hyderabad)
        activeTrip = new Trip(
                "TRIP-2026-9001",
                driver,
                ownerLorry,
                owner,
                "Nellore",
                "Hyderabad",
                TripStatus.IN_PROGRESS,
                LocalDate.now(),
                Instant.now(),
                null,
                new BigDecimal("480.00"),
                "Cotton Bales Express"
        );
        activeTrip.setTripAmount(new BigDecimal("45000.00")); // Gross Revenue
        activeTrip = tripRepository.save(activeTrip);
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        cleanupDatabase();
    }

    private void cleanupDatabase() {
        financeTransactionRepository.deleteAll();
        maintenanceRecordRepository.deleteAll();
        tripRepository.deleteAll();
        lorryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/owner/trips - Should return paginated trips for owner with status/search filters")
    void getOwnerTrips_shouldReturnPaginatedTrips() throws Exception {
        // Create second completed trip
        Trip completedTrip = new Trip(
                "TRIP-2026-9002",
                driver,
                ownerLorry,
                owner,
                "Chennai",
                "Bangalore",
                TripStatus.COMPLETED,
                LocalDate.now().minusDays(3),
                Instant.now().minusSeconds(86400 * 3),
                Instant.now().minusSeconds(86400 * 2),
                new BigDecimal("350.00"),
                "Auto Parts"
        );
        completedTrip.setTripAmount(new BigDecimal("32000.00"));
        tripRepository.save(completedTrip);

        // 1. Fetch all trips
        mockMvc.perform(get("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.trips", hasSize(2)))
                .andExpect(jsonPath("$.trips[0].lorryRegistration", is("AP 39 XX 1234")))
                .andExpect(jsonPath("$.trips[0].driverName", is("Ramesh Kumar")));

        // 2. Filter by status COMPLETED
        mockMvc.perform(get("/api/owner/trips?status=COMPLETED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.trips[0].tripReference", is("TRIP-2026-9002")));

        // 3. Search by route "Hyderabad"
        mockMvc.perform(get("/api/owner/trips?search=Hyderabad")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.trips[0].destinationLocation", is("Hyderabad")));
    }

    @Test
    @DisplayName("POST /api/owner/trips/{id}/maintenance - Automatically derives lorry from trip")
    void addTripMaintenance_shouldAutomaticallyDeriveLorry() throws Exception {
        CreateMaintenanceRecordRequest request = new CreateMaintenanceRecordRequest(
                null, // Lorry ID omitted; automatically derived!
                "Engine Oil Change",
                LocalDate.now(),
                "Mobil 1 15W-40 Synthetic Oil replacement at midway workshop",
                new BigDecimal("2000.00"),
                145000,
                LocalDate.now().plusMonths(3),
                "BHP Highway Service Hub",
                "Routine oil change during Nellore-Hyderabad trip"
        );

        mockMvc.perform(post("/api/owner/trips/" + activeTrip.getId() + "/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.lorryId", is(ownerLorry.getId().intValue())))
                .andExpect(jsonPath("$.lorryRegistration", is("AP 39 XX 1234")))
                .andExpect(jsonPath("$.tripId", is(activeTrip.getId().intValue())))
                .andExpect(jsonPath("$.tripReference", is("TRIP-2026-9001")))
                .andExpect(jsonPath("$.cost", is(2000.00)));
    }

    @Test
    @DisplayName("POST /api/owner/trips/{id}/maintenance - Rejects mismatched lorry ID")
    void addTripMaintenance_withMismatchedLorry_shouldReturn400() throws Exception {
        CreateMaintenanceRecordRequest request = new CreateMaintenanceRecordRequest(
                99999L, // Explicitly mismatched lorry ID
                "Tyre Repair",
                LocalDate.now(),
                "Puncture fix",
                new BigDecimal("800.00"),
                145000,
                null,
                "MRF Tyre Hub",
                null
        );

        mockMvc.perform(post("/api/owner/trips/" + activeTrip.getId() + "/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Dual Visibility: Maintenance logged in Trip must appear in Lorry Maintenance History")
    void maintenanceLoggedInTrip_shouldAppearInLorryMaintenanceHistory() throws Exception {
        // 1. Log maintenance on trip
        CreateMaintenanceRecordRequest request = new CreateMaintenanceRecordRequest(
                null,
                "Tyre Replacement",
                LocalDate.now(),
                "Front-left tyre replaced",
                new BigDecimal("12500.00"),
                145200,
                LocalDate.now().plusMonths(6),
                "Apollo Tyres Nellore",
                "New radial tyre"
        );

        mockMvc.perform(post("/api/owner/trips/" + activeTrip.getId() + "/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2. Inspect Lorry Details Maintenance endpoint GET /api/owner/lorries/{id}/maintenance
        mockMvc.perform(get("/api/owner/lorries/" + ownerLorry.getId() + "/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(1)))
                .andExpect(jsonPath("$.records[0].serviceType", is("Tyre Replacement")))
                .andExpect(jsonPath("$.records[0].tripId", is(activeTrip.getId().intValue())))
                .andExpect(jsonPath("$.records[0].tripReference", is("TRIP-2026-9001")));
    }

    @Test
    @DisplayName("GET /api/owner/trips/{id}/details - Calculates accurate Profit = Revenue - Total Expenses")
    void getTripDetails_shouldCalculateAccurateProfit() throws Exception {
        // 1. Add Maintenance: Engine oil ₹2,000 + Tyre ₹800
        CreateMaintenanceRecordRequest mr1 = new CreateMaintenanceRecordRequest(
                null, "Engine Oil", LocalDate.now(), "Oil change", new BigDecimal("2000.00"), 145000, null, "Castrol Hub", null
        );
        CreateMaintenanceRecordRequest mr2 = new CreateMaintenanceRecordRequest(
                null, "Tyre Repair", LocalDate.now(), "Tyre puncture", new BigDecimal("800.00"), 145000, null, "MRF", null
        );
        mockMvc.perform(post("/api/owner/trips/" + activeTrip.getId() + "/maintenance")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mr1))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/owner/trips/" + activeTrip.getId() + "/maintenance")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mr2))).andExpect(status().isCreated());

        // 2. Add Fuel Expense: Diesel ₹12,000
        CreateTripExpenseRequest fuelExp = new CreateTripExpenseRequest(
                "DIESEL", "Diesel filling at Indian Oil", new BigDecimal("12000.00"), LocalDate.now(), "Tank full", null, null
        );
        mockMvc.perform(post("/api/owner/trips/" + activeTrip.getId() + "/expenses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fuelExp))).andExpect(status().isCreated());

        // 3. Add Toll Expense: Toll ₹1,500
        CreateTripExpenseRequest tollExp = new CreateTripExpenseRequest(
                "TOLL", "Fastag toll charges", new BigDecimal("1500.00"), LocalDate.now(), "NH-44 Tolls", null, null
        );
        mockMvc.perform(post("/api/owner/trips/" + activeTrip.getId() + "/expenses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tollExp))).andExpect(status().isCreated());

        // 4. Add Driver Allowance: Driver Salary/Bata ₹2,500
        CreateTripExpenseRequest driverExp = new CreateTripExpenseRequest(
                "DRIVER_SALARY", "Trip Driver Bata / Food Allowance", new BigDecimal("2500.00"), LocalDate.now(), "Cash given", null, null
        );
        mockMvc.perform(post("/api/owner/trips/" + activeTrip.getId() + "/expenses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(driverExp))).andExpect(status().isCreated());

        // Verification:
        // Revenue = ₹45,000.00
        // Maintenance = ₹2,000 + ₹800 = ₹2,800.00
        // Fuel = ₹12,000.00
        // Driver = ₹2,500.00
        // Other = ₹1,500.00 (Toll)
        // Total Expenses = ₹18,800.00
        // Net Profit = ₹45,000 - ₹18,800 = ₹26,200.00
        mockMvc.perform(get("/api/owner/trips/" + activeTrip.getId() + "/details")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripRevenue", is(45000.00)))
                .andExpect(jsonPath("$.maintenanceExpenses", is(2800.00)))
                .andExpect(jsonPath("$.fuelExpenses", is(12000.00)))
                .andExpect(jsonPath("$.driverExpenses", is(2500.00)))
                .andExpect(jsonPath("$.otherExpenses", is(1500.00)))
                .andExpect(jsonPath("$.totalExpenses", is(18800.00)))
                .andExpect(jsonPath("$.netProfit", is(26200.00)))
                .andExpect(jsonPath("$.maintenanceRecords", hasSize(2)))
                .andExpect(jsonPath("$.otherExpensesList", hasSize(3)));
    }

    @Test
    @DisplayName("IDOR Security: Cross-owner trip access is strictly forbidden")
    void crossOwnerAccess_shouldReturnForbidden() throws Exception {
        // Other owner attempts to view primary owner's trip details
        mockMvc.perform(get("/api/owner/trips/" + activeTrip.getId() + "/details")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken))
                .andExpect(status().isForbidden());

        // Driver attempts to access owner trip endpoint
        mockMvc.perform(get("/api/owner/trips/" + activeTrip.getId() + "/details")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/owner/trips/{tripId}/maintenance/{id} - Allows owner to delete trip maintenance")
    void deleteTripMaintenance_shouldSucceed() throws Exception {
        CreateMaintenanceRecordRequest request = new CreateMaintenanceRecordRequest(
                null, "Puncture", LocalDate.now(), "Fix", new BigDecimal("300.00"), 145000, null, "Puncture Shop", null
        );

        String responseJson = mockMvc.perform(post("/api/owner/trips/" + activeTrip.getId() + "/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long recordId = objectMapper.readTree(responseJson).get("id").asLong();

        // Delete record
        mockMvc.perform(delete("/api/owner/trips/" + activeTrip.getId() + "/maintenance/" + recordId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        // Verify it was removed
        mockMvc.perform(get("/api/owner/trips/" + activeTrip.getId() + "/details")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintenanceRecords", hasSize(0)));
    }
}
