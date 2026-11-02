package com.lorryowner.backend.controller;

import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.OperationalStatus;
import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.UserRepository;
import com.lorryowner.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OwnerDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LorryRepository lorryRepository;

    @Autowired
    private com.lorryowner.backend.repository.TripRepository tripRepository;

    @Autowired
    private com.lorryowner.backend.repository.MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String ownerToken;
    private String driverToken;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository.deleteAll();
        tripRepository.deleteAll();
        userRepository.deleteAll();
        lorryRepository.deleteAll();

        // 1. Create Owner User
        User owner = new User(
                "srinu_owner",
                passwordEncoder.encode("OwnerPassword123!"),
                Role.OWNER,
                "Srinivasulu Koduru",
                "9876543210",
                "srinu@bhp.com"
        );
        owner = userRepository.save(owner);
        ownerToken = jwtService.generateToken(owner);

        // 2. Create Driver User
        User driver = new User(
                "driver_ramesh",
                passwordEncoder.encode("DriverPass123!"),
                Role.DRIVER,
                "Ramesh Kumar",
                "9876543211",
                null
        );
        driver = userRepository.save(driver);
        driverToken = jwtService.generateToken(driver);

        // 3. Populate 5 fleet lorries with realistic states
        saveLorry("AP 31 TC 1234", "Ashok Leyland 2820 HG", "20.00", OperationalStatus.ON_TRIP, owner);
        saveLorry("AP 26 TD 5678", "Ashok Leyland 3520 8x2", "25.00", OperationalStatus.UNAVAILABLE, owner);
        saveLorry("TS 09 UA 9012", "Ashok Leyland 4220 10x2", "30.00", OperationalStatus.ON_TRIP, owner);
        saveLorry("AP 39 TT 3456", "Ashok Leyland 2820 HG", "20.00", OperationalStatus.ON_TRIP, owner);
        saveLorry("AP 26 TE 7890", "Ashok Leyland 1920 4x2", "14.00", OperationalStatus.AVAILABLE, owner);
    }

    private void saveLorry(String reg, String model, String capacity, OperationalStatus status, User owner) {
        Lorry lorry = new Lorry(reg, model, new BigDecimal(capacity), status);
        lorry.setOwner(owner);
        lorryRepository.save(lorry);
    }

    @Test
    @DisplayName("Owner can fetch dashboard overview with real fleet metrics")
    void getDashboard_asOwner_shouldReturnFullOverview() throws Exception {
        mockMvc.perform(get("/api/owner/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerName").value("Srinivasulu Koduru"))
                .andExpect(jsonPath("$.unreadNotificationsCount").value(1))
                .andExpect(jsonPath("$.fleetOverview.runningNow").value(3))
                .andExpect(jsonPath("$.fleetOverview.readyToStart").value(1))
                .andExpect(jsonPath("$.fleetOverview.notReady").value(1))
                .andExpect(jsonPath("$.fleetOverview.totalLorries").value(5))
                .andExpect(jsonPath("$.financialSummary.monthLabel").value("This month"))
                .andExpect(jsonPath("$.financialSummary.isSample").value(true))
                .andExpect(jsonPath("$.financialSummary.income").value(389500.00))
                .andExpect(jsonPath("$.recentActivities", hasSize(5)));
    }

    @Test
    @DisplayName("Owner can fetch fleet lorries list")
    void getAllLorries_asOwner_shouldReturnLorries() throws Exception {
        mockMvc.perform(get("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].registrationNumber").isNotEmpty());
    }

    @Test
    @DisplayName("Driver role cannot access owner dashboard (403 Forbidden)")
    void getDashboard_asDriver_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/owner/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Driver role cannot access owner lorries (403 Forbidden)")
    void getAllLorries_asDriver_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Unauthenticated request to owner dashboard returns 401 Unauthorized")
    void getDashboard_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/owner/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        maintenanceRecordRepository.deleteAll();
        tripRepository.deleteAll();
        userRepository.deleteAll();
        lorryRepository.deleteAll();
    }
}
