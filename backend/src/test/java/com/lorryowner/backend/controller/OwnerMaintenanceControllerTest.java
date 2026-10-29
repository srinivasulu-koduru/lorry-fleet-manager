package com.lorryowner.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorryowner.backend.dto.maintenance.CreateMaintenanceRecordRequest;
import com.lorryowner.backend.entity.*;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.MaintenanceRecordRepository;
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
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OwnerMaintenanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LorryRepository lorryRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User owner1;
    private User owner2;
    private User driver;
    private Lorry lorry1;
    private Lorry lorry2;
    private String owner1Token;
    private String owner2Token;
    private String driverToken;

    @BeforeEach
    void setUp() {
        tearDown();

        owner1 = new User("maint_test_owner1", passwordEncoder.encode("OwnerPass123!"), Role.OWNER, "Primary Owner", "9876543001", null);
        owner1 = userRepository.save(owner1);

        owner2 = new User("maint_test_owner2", passwordEncoder.encode("OwnerPass123!"), Role.OWNER, "Secondary Owner", "9876543002", null);
        owner2 = userRepository.save(owner2);

        driver = new User("maint_test_driver", passwordEncoder.encode("DriverPass123!"), Role.DRIVER, "Test Driver", "9876543003", "DL-M-001");
        driver.setOwner(owner1);
        driver = userRepository.save(driver);

        lorry1 = new Lorry("AP 02 AB 1234", "Tata Commercial Vehicle", new BigDecimal("28.0"), OperationalStatus.AVAILABLE);
        lorry1.setOwner(owner1);
        lorry1 = lorryRepository.save(lorry1);

        lorry2 = new Lorry("AP 02 CD 5678", "Ashok Leyland 3520", new BigDecimal("35.0"), OperationalStatus.AVAILABLE);
        lorry2.setOwner(owner2);
        lorry2 = lorryRepository.save(lorry2);

        owner1Token = jwtService.generateToken(owner1);
        owner2Token = jwtService.generateToken(owner2);
        driverToken = jwtService.generateToken(driver);
    }

    @AfterEach
    void tearDown() {
        maintenanceRecordRepository.deleteAll();
        lorryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Owner can get maintenance summary with accurate status counts")
    void getMaintenanceSummary_returnsAccurateCounts() throws Exception {
        LocalDate today = LocalDate.now();

        // Record for lorry 1: overdue service
        MaintenanceRecord r1 = new MaintenanceRecord(
                lorry1, owner1, today.minusDays(60), "Brake Service",
                "Replaced brake pads", new BigDecimal("4500.00"), "SRS Auto", 40000,
                today.minusDays(10) // Overdue
        );
        maintenanceRecordRepository.save(r1);

        mockMvc.perform(get("/api/owner/maintenance/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVehicles").value(1))
                .andExpect(jsonPath("$.overdueCount").value(1))
                .andExpect(jsonPath("$.dueSoonCount").value(0))
                .andExpect(jsonPath("$.totalExpenditure").value(4500.00))
                .andExpect(jsonPath("$.vehicles", hasSize(1)))
                .andExpect(jsonPath("$.vehicles[0].registrationNumber").value("AP 02 AB 1234"))
                .andExpect(jsonPath("$.vehicles[0].status").value("Overdue"));
    }

    @Test
    @DisplayName("Owner can create a valid maintenance record via POST /api/owner/maintenance")
    void createMaintenanceRecord_success() throws Exception {
        LocalDate today = LocalDate.now();
        CreateMaintenanceRecordRequest request = new CreateMaintenanceRecordRequest(
                lorry1.getId(),
                "Engine Repair",
                today,
                "Engine oil change and filter replacement",
                new BigDecimal("8500.00"),
                45000,
                today.plusDays(90),
                "BHP Central Workshop",
                "Engine running smoothly"
        );

        mockMvc.perform(post("/api/owner/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lorryId").value(lorry1.getId()))
                .andExpect(jsonPath("$.serviceType").value("Engine Repair"))
                .andExpect(jsonPath("$.cost").value(8500.00))
                .andExpect(jsonPath("$.odometerReadingKm").value(45000))
                .andExpect(jsonPath("$.serviceCenter").value("BHP Central Workshop"));
    }

    @Test
    @DisplayName("Owner can create maintenance record via POST /api/owner/lorries/{id}/maintenance")
    void createLorryMaintenance_nestedPath_success() throws Exception {
        LocalDate today = LocalDate.now();
        CreateMaintenanceRecordRequest request = new CreateMaintenanceRecordRequest(
                null,
                "Regular Service",
                today,
                "Routine 50,000 km inspection",
                new BigDecimal("6200.00"),
                50000,
                today.plusDays(60),
                "Authorized Service Center",
                null
        );

        mockMvc.perform(post("/api/owner/lorries/" + lorry1.getId() + "/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lorryId").value(lorry1.getId()))
                .andExpect(jsonPath("$.serviceType").value("Regular Service"))
                .andExpect(jsonPath("$.cost").value(6200.00));
    }

    @Test
    @DisplayName("Owner can get vehicle maintenance details and history")
    void getVehicleMaintenanceDetails_success() throws Exception {
        LocalDate today = LocalDate.now();
        MaintenanceRecord r1 = new MaintenanceRecord(
                lorry1, owner1, today.minusDays(30), "Tyre Replacement",
                "Replaced 2 front tyres", new BigDecimal("12000.00"), "MRF Plaza", 42000,
                today.plusDays(150)
        );
        maintenanceRecordRepository.save(r1);

        mockMvc.perform(get("/api/owner/maintenance/vehicles/" + lorry1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lorryId").value(lorry1.getId()))
                .andExpect(jsonPath("$.registrationNumber").value("AP 02 AB 1234"))
                .andExpect(jsonPath("$.makeModel").value("Tata Commercial Vehicle"))
                .andExpect(jsonPath("$.totalMaintenanceCost").value(12000.00))
                .andExpect(jsonPath("$.records", hasSize(1)))
                .andExpect(jsonPath("$.records[0].serviceType").value("Tyre Replacement"));
    }

    @Test
    @DisplayName("Cross-owner protection: Owner cannot get details of another owner's lorry")
    void crossOwnerAccess_returnsForbidden() throws Exception {
        // Owner 1 attempts to access lorry 2 (owned by owner 2)
        mockMvc.perform(get("/api/owner/maintenance/vehicles/" + lorry2.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-owner protection: Owner cannot create maintenance record for another owner's lorry")
    void crossOwnerCreate_returnsForbidden() throws Exception {
        LocalDate today = LocalDate.now();
        CreateMaintenanceRecordRequest request = new CreateMaintenanceRecordRequest(
                lorry2.getId(),
                "Oil Change",
                today,
                "Unauthorized service attempt",
                new BigDecimal("3000.00"),
                30000,
                today.plusDays(60),
                "Unknown Garage",
                null
        );

        mockMvc.perform(post("/api/owner/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Role protection: Driver cannot access owner maintenance endpoints")
    void driverRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/owner/maintenance/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/owner/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authentication protection: Unauthenticated request returns 401")
    void unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/owner/maintenance/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Validation: Negative cost is rejected with 400 Bad Request")
    void negativeCost_returnsBadRequest() throws Exception {
        LocalDate today = LocalDate.now();
        CreateMaintenanceRecordRequest request = new CreateMaintenanceRecordRequest(
                lorry1.getId(),
                "Service",
                today,
                "Negative cost test",
                new BigDecimal("-500.00"),
                10000,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/owner/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Validation: Negative odometer is rejected with 400 Bad Request")
    void negativeOdometer_returnsBadRequest() throws Exception {
        LocalDate today = LocalDate.now();
        CreateMaintenanceRecordRequest request = new CreateMaintenanceRecordRequest(
                lorry1.getId(),
                "Service",
                today,
                "Negative odometer test",
                new BigDecimal("500.00"),
                -100,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/owner/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Validation: Next service date before service date is rejected with 400 Bad Request")
    void nextServiceBeforeMaintenanceDate_returnsBadRequest() throws Exception {
        LocalDate today = LocalDate.now();
        CreateMaintenanceRecordRequest request = new CreateMaintenanceRecordRequest(
                lorry1.getId(),
                "Service",
                today,
                "Invalid date order test",
                new BigDecimal("1000.00"),
                10000,
                today.minusDays(5), // Before maintenance date
                null,
                null
        );

        mockMvc.perform(post("/api/owner/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Nonexistent lorry returns 404 Not Found")
    void nonexistentLorry_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/owner/maintenance/vehicles/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isNotFound());
    }
}
