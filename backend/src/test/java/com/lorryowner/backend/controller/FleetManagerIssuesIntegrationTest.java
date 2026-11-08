package com.lorryowner.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorryowner.backend.dto.driver.UpdateTripNotesRequest;
import com.lorryowner.backend.dto.finance.CreateFinanceTransactionRequest;
import com.lorryowner.backend.dto.trip.TripCreateDto;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FleetManagerIssuesIntegrationTest {

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
    private FinanceTransactionRepository financeTransactionRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User owner;
    private User driver1;
    private User driver2;
    private Lorry lorry1;
    private Lorry lorry2;
    private String ownerToken;
    private String driver1Token;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        cleanup();

        owner = new User("issues_owner", passwordEncoder.encode("Pass123!"), Role.OWNER, "BHP Logistics Owner", "9876543210", "owner@bhp.com");
        owner = userRepository.save(owner);
        ownerToken = jwtService.generateToken(owner);

        driver1 = new User("issues_driver1", passwordEncoder.encode("Pass123!"), Role.DRIVER, "Driver Suresh", "9876543211", "DL-99901");
        driver1.setOwner(owner);
        driver1 = userRepository.save(driver1);
        driver1Token = jwtService.generateToken(driver1);

        driver2 = new User("issues_driver2", passwordEncoder.encode("Pass123!"), Role.DRIVER, "Driver Ramesh", "9876543212", "DL-99902");
        driver2.setOwner(owner);
        driver2 = userRepository.save(driver2);

        lorry1 = new Lorry("AP 01 AB 1234", "Ashok Leyland 4220", new BigDecimal("25.00"), OperationalStatus.AVAILABLE);
        lorry1.setOwner(owner);
        lorry1 = lorryRepository.save(lorry1);

        lorry2 = new Lorry("AP 01 CD 5678", "Tata Signa 4923", new BigDecimal("30.00"), OperationalStatus.AVAILABLE);
        lorry2.setOwner(owner);
        lorry2 = lorryRepository.save(lorry2);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        cleanup();
    }

    private void cleanup() {
        financeTransactionRepository.deleteAll();
        tripRepository.deleteAll();
        maintenanceRecordRepository.deleteAll();
        lorryRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // ISSUE 1: Fleet Overview Tests
    // =========================================================================
    @Test
    @DisplayName("Fleet Overview: Lorry addition, Trip start (Running Now increases), Trip completion (Running Now decreases)")
    void testFleetOverview_LifecycleRunningCounts() throws Exception {
        // 1. Initial State: 2 lorries, 0 running, 2 ready to start, 0 not ready
        mockMvc.perform(get("/api/owner/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fleetOverview.totalLorries").value(2))
                .andExpect(jsonPath("$.fleetOverview.runningNow").value(0))
                .andExpect(jsonPath("$.fleetOverview.readyToStart").value(2))
                .andExpect(jsonPath("$.fleetOverview.notReady").value(0));

        // 2. Create a trip for lorry1 with driver1
        TripCreateDto tripRequest = new TripCreateDto(
                lorry1.getId(),
                driver1.getId(),
                "Hyderabad",
                "Nellore",
                LocalDate.now(),
                LocalTime.of(9, 0),
                null,
                null,
                "General Cargo",
                "Priority dispatch",
                new BigDecimal("25000.00")
        );

        String createJson = mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tripRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripAmount").value(25000.00))
                .andReturn().getResponse().getContentAsString();

        Long tripId = objectMapper.readTree(createJson).get("id").asLong();

        // 3. Start the trip (Driver starts or Owner starts) -> Running Now must become 1, Ready to Start must become 1
        mockMvc.perform(post("/api/owner/trips/" + tripId + "/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/owner/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fleetOverview.totalLorries").value(2))
                .andExpect(jsonPath("$.fleetOverview.runningNow").value(1))
                .andExpect(jsonPath("$.fleetOverview.readyToStart").value(1))
                .andExpect(jsonPath("$.fleetOverview.notReady").value(0));

        // 4. Complete the trip -> Running Now must decrease back to 0, Ready to Start becomes 2
        mockMvc.perform(post("/api/owner/trips/" + tripId + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTripNotesRequest("Delivered successfully"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/owner/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fleetOverview.totalLorries").value(2))
                .andExpect(jsonPath("$.fleetOverview.runningNow").value(0))
                .andExpect(jsonPath("$.fleetOverview.readyToStart").value(2))
                .andExpect(jsonPath("$.fleetOverview.notReady").value(0));
    }

    // =========================================================================
    // ISSUE 2 & 3: Trip Amount as Income & Dynamic Finance Tests
    // =========================================================================
    @Test
    @DisplayName("Finance: Create trip with ₹25,000 amount -> Record ₹5,000 expense -> Net profit is ₹20,000")
    void testFinance_TripAmountIncome_Expense_NetProfit() throws Exception {
        // 1. Create a trip with ₹25,000 trip amount
        TripCreateDto tripRequest = new TripCreateDto(
                lorry1.getId(),
                driver1.getId(),
                "Hyderabad",
                "Nellore",
                LocalDate.now(),
                LocalTime.of(10, 0),
                null,
                null,
                "Electronics",
                "Handled with care",
                new BigDecimal("25000.00")
        );

        String createJson = mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tripRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripAmount").value(25000.00))
                .andReturn().getResponse().getContentAsString();

        Long tripId = objectMapper.readTree(createJson).get("id").asLong();

        // 2. Start and Complete the trip
        mockMvc.perform(post("/api/owner/trips/" + tripId + "/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/owner/trips/" + tripId + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk());

        // Verify income was recorded exactly once
        assertEquals(1, financeTransactionRepository.count());
        FinanceTransaction incomeTx = financeTransactionRepository.findAll().get(0);
        assertEquals("INCOME", incomeTx.getTransactionType());
        assertEquals("FREIGHT", incomeTx.getCategory());
        assertEquals(new BigDecimal("25000.00"), incomeTx.getAmount());

        // Calling complete again idempotently must not double-count income
        mockMvc.perform(post("/api/owner/trips/" + tripId + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk());
        assertEquals(1, financeTransactionRepository.count());

        // 3. Record ₹5,000 expenses (Diesel)
        CreateFinanceTransactionRequest expenseRequest = new CreateFinanceTransactionRequest(
                "EXPENSE",
                "DIESEL",
                "Fuel refilling Hyderabad",
                new BigDecimal("5000.00"),
                LocalDate.now(),
                "PAID",
                lorry1.getId(),
                tripId,
                "UPI",
                "TXN-DIESEL-01"
        );

        mockMvc.perform(post("/api/owner/finance/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseRequest)))
                .andExpect(status().isCreated());

        // 4. Verify Dashboard Financial Summary:
        // Total Income: 25000
        // Total Expenses: 5000
        // Net Profit: 20000
        // Total Trips: 1
        mockMvc.perform(get("/api/owner/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financialSummary.income").value(25000.00))
                .andExpect(jsonPath("$.financialSummary.expenses").value(5000.00))
                .andExpect(jsonPath("$.financialSummary.profit").value(20000.00))
                .andExpect(jsonPath("$.financialSummary.totalTrips").value(1))
                .andExpect(jsonPath("$.financialSummary.isSample").value(false));
    }

    // =========================================================================
    // ISSUE 4: Resource Availability Enforcement Tests
    // =========================================================================
    @Test
    @DisplayName("Resource Availability: Running lorry and driver excluded from available list and rejected on trip create")
    void testResourceAvailability_PreventsReassignmentWhileActive() throws Exception {
        // 1. Initial State: Both lorries and drivers are available
        mockMvc.perform(get("/api/owner/lorries/available")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/owner/drivers/available")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 2. Create and start an active trip with lorry1 and driver1
        TripCreateDto trip1 = new TripCreateDto(
                lorry1.getId(),
                driver1.getId(),
                "Hyderabad",
                "Nellore",
                LocalDate.now(),
                LocalTime.of(8, 0),
                null,
                null,
                "General Goods",
                null,
                new BigDecimal("15000.00")
        );

        String res1 = mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(trip1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long trip1Id = objectMapper.readTree(res1).get("id").asLong();

        mockMvc.perform(post("/api/owner/trips/" + trip1Id + "/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk());

        // 3. Verify available endpoints exclude the running lorry1 and running driver1
        mockMvc.perform(get("/api/owner/lorries/available")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(lorry2.getId()));

        mockMvc.perform(get("/api/owner/drivers/available")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(driver2.getId()));

        // 4. Verify backend rejects creating another trip with lorry1 (already running)
        TripCreateDto tripConflictLorry = new TripCreateDto(
                lorry1.getId(),
                driver2.getId(), // Available driver, but busy lorry
                "Hyderabad",
                "Kurnool",
                LocalDate.now(),
                LocalTime.of(12, 0),
                null,
                null,
                "Cargo",
                null,
                new BigDecimal("10000.00")
        );

        mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tripConflictLorry)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("This lorry is already assigned to an active trip. Please select another lorry.")));

        // 5. Verify backend rejects creating another trip with driver1 (already running)
        TripCreateDto tripConflictDriver = new TripCreateDto(
                lorry2.getId(), // Available lorry, but busy driver
                driver1.getId(),
                "Hyderabad",
                "Kurnool",
                LocalDate.now(),
                LocalTime.of(12, 0),
                null,
                null,
                "Cargo",
                null,
                new BigDecimal("10000.00")
        );

        mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tripConflictDriver)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("This driver is already assigned to an active trip. Please select another driver.")));

        // 6. Complete trip1 -> lorry1 and driver1 become available again
        mockMvc.perform(post("/api/owner/trips/" + trip1Id + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/owner/lorries/available")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/owner/drivers/available")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
