package com.lorryowner.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorryowner.backend.entity.*;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DriverTripControllerTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User owner;
    private User driver1;
    private User driver2;
    private Lorry testLorry;
    private Trip testTrip1;
    private Trip testTrip2;
    private String driver1Token;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        tearDown();

        owner = new User("test_owner_driver_ctrl", passwordEncoder.encode("OwnerPass123!"), Role.OWNER, "Test Owner", "9998887771", null);
        owner = userRepository.save(owner);

        testLorry = new Lorry("TEST-LRY-001", "Tata Signa 4825.TK", new BigDecimal("25.0"), OperationalStatus.AVAILABLE);
        testLorry.setOwner(owner);
        testLorry = lorryRepository.save(testLorry);

        driver1 = new User("test_driver_one", passwordEncoder.encode("DriverPass123!"), Role.DRIVER, "Ramesh Driver 1", "9876543210", "DL-TEST-001");
        driver1.setOwner(owner);
        driver1.setAssignedLorry(testLorry);
        driver1 = userRepository.save(driver1);

        driver2 = new User("test_driver_two", passwordEncoder.encode("DriverPass123!"), Role.DRIVER, "Suresh Driver 2", "9876543211", "DL-TEST-002");
        driver2.setOwner(owner);
        driver2 = userRepository.save(driver2);

        testTrip1 = new Trip(
                "TRIP-TEST-D1",
                driver1,
                testLorry,
                owner,
                "Hyderabad Depot",
                "Bengaluru Hub",
                TripStatus.IN_PROGRESS,
                LocalDate.now(),
                Instant.now(),
                null,
                new BigDecimal("570.00"),
                "Urgent delivery"
        );
        testTrip1 = tripRepository.save(testTrip1);

        testTrip2 = new Trip(
                "TRIP-TEST-D2",
                driver2,
                testLorry,
                owner,
                "Chennai Port",
                "Coimbatore",
                TripStatus.PLANNED,
                LocalDate.now().plusDays(1),
                null,
                null,
                new BigDecimal("490.00"),
                "Driver 2 private trip"
        );
        testTrip2 = tripRepository.save(testTrip2);

        driver1Token = jwtService.generateToken(driver1);
        ownerToken = jwtService.generateToken(owner);
    }

    @AfterEach
    void tearDown() {
        tripRepository.deleteAll(tripRepository.findAll().stream()
                .filter(t -> t.getTripReference() != null && t.getTripReference().startsWith("TRIP-TEST-D"))
                .toList());

        lorryRepository.findByRegistrationNumber("TEST-LRY-001").ifPresent(lorry -> {
            userRepository.findByAssignedLorry(lorry).ifPresent(d -> {
                d.setAssignedLorry(null);
                userRepository.save(d);
            });
            lorryRepository.delete(lorry);
        });

        userRepository.findByUsername("test_driver_one").ifPresent(userRepository::delete);
        userRepository.findByUsername("test_driver_two").ifPresent(userRepository::delete);
        userRepository.findByUsername("test_owner_driver_ctrl").ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("Driver can successfully fetch dashboard metrics and active trip")
    void testGetDriverDashboard_Success() throws Exception {
        mockMvc.perform(get("/api/driver/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverName", is("Ramesh Driver 1")))
                .andExpect(jsonPath("$.driverUsername", is("test_driver_one")))
                .andExpect(jsonPath("$.assignedLorryRegistration", is("TEST-LRY-001")))
                .andExpect(jsonPath("$.assignedLorryMakeModel", is("Tata Signa 4825.TK")))
                .andExpect(jsonPath("$.currentTripsCount", is(1)))
                .andExpect(jsonPath("$.todayTrip.tripReference", is("TRIP-TEST-D1")))
                .andExpect(jsonPath("$.todayTrip.originLocation", is("Hyderabad Depot")))
                .andExpect(jsonPath("$.todayTrip.destinationLocation", is("Bengaluru Hub")));
    }

    @Test
    @DisplayName("Driver can list only their assigned trips")
    void testGetDriverTrips_ReturnsOnlyOwnTrips() throws Exception {
        mockMvc.perform(get("/api/driver/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tripReference", is("TRIP-TEST-D1")));
    }

    @Test
    @DisplayName("Driver can fetch details of their own trip")
    void testGetTripDetails_Success() throws Exception {
        mockMvc.perform(get("/api/driver/trips/" + testTrip1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripReference", is("TRIP-TEST-D1")))
                .andExpect(jsonPath("$.originLocation", is("Hyderabad Depot")))
                .andExpect(jsonPath("$.destinationLocation", is("Bengaluru Hub")));
    }

    @Test
    @DisplayName("Driver cannot access another driver's trip (enforces record-level authorization)")
    void testGetTripDetails_OtherDriverTrip_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/driver/trips/" + testTrip2.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Driver can fetch their profile details")
    void testGetDriverProfile_Success() throws Exception {
        mockMvc.perform(get("/api/driver/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Ramesh Driver 1")))
                .andExpect(jsonPath("$.role", is("DRIVER")))
                .andExpect(jsonPath("$.phoneNumber", is("9876543210")))
                .andExpect(jsonPath("$.assignedLorryRegistration", is("TEST-LRY-001")));
    }

    @Test
    @DisplayName("Unauthenticated request to driver dashboard is rejected with 401")
    void testGetDriverDashboard_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/driver/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Owner role cannot access driver-only endpoints (returns 403)")
    void testGetDriverDashboard_OwnerToken_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/driver/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Driver can start an assigned planned trip")
    void testStartTrip_Success() throws Exception {
        // Create planned trip for driver 2 who has no active trip
        Trip plannedTrip = new Trip(
                "TRIP-TEST-D2-PLAN",
                driver2,
                testLorry,
                owner,
                "Vijayawada Hub",
                "Guntur Depot",
                TripStatus.PLANNED,
                LocalDate.now(),
                null,
                null,
                new BigDecimal("40.00"),
                "Short dispatch"
        );
        plannedTrip = tripRepository.save(plannedTrip);

        String driver2Token = jwtService.generateToken(driver2);

        mockMvc.perform(post("/api/driver/trips/" + plannedTrip.getId() + "/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripReference", is("TRIP-TEST-D2-PLAN")))
                .andExpect(jsonPath("$.tripStatus", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.startTime", notNullValue()));
    }

    @Test
    @DisplayName("Driver cannot start another driver's trip (record-level security)")
    void testStartTrip_OtherDriverTrip_ReturnsNotFound() throws Exception {
        // testTrip2 belongs to driver 2; driver 1 attempts to start it
        mockMvc.perform(post("/api/driver/trips/" + testTrip2.getId() + "/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Starting an already IN_PROGRESS trip is idempotent")
    void testStartTrip_Idempotent() throws Exception {
        // testTrip1 is already IN_PROGRESS for driver 1
        mockMvc.perform(post("/api/driver/trips/" + testTrip1.getId() + "/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripStatus", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("Starting an already COMPLETED trip is rejected with 400 Bad Request")
    void testStartTrip_AlreadyCompleted_ReturnsBadRequest() throws Exception {
        Trip completedTrip = new Trip(
                "TRIP-TEST-D1-COMP",
                driver1,
                testLorry,
                owner,
                "Hyd",
                "Blr",
                TripStatus.COMPLETED,
                LocalDate.now(),
                Instant.now().minusSeconds(3600),
                Instant.now(),
                new BigDecimal("500.00"),
                "Done"
        );
        completedTrip = tripRepository.save(completedTrip);

        mockMvc.perform(post("/api/driver/trips/" + completedTrip.getId() + "/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Driver can complete an assigned IN_PROGRESS trip with optional delivery notes")
    void testCompleteTrip_Success() throws Exception {
        String requestJson = objectMapper.writeValueAsString(new com.lorryowner.backend.dto.driver.UpdateTripNotesRequest("Delivered safely to gate 3"));

        mockMvc.perform(post("/api/driver/trips/" + testTrip1.getId() + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripReference", is("TRIP-TEST-D1")))
                .andExpect(jsonPath("$.tripStatus", is("COMPLETED")))
                .andExpect(jsonPath("$.endTime", notNullValue()))
                .andExpect(jsonPath("$.deliveryNotes", containsString("Delivered safely to gate 3")));
    }

    @Test
    @DisplayName("Completing a PLANNED trip without starting is rejected with 400 Bad Request")
    void testCompleteTrip_PlannedTrip_ReturnsBadRequest() throws Exception {
        String driver2Token = jwtService.generateToken(driver2);

        // testTrip2 is in PLANNED status
        mockMvc.perform(post("/api/driver/trips/" + testTrip2.getId() + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver2Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Driver cannot complete another driver's trip (record-level security)")
    void testCompleteTrip_OtherDriverTrip_ReturnsNotFound() throws Exception {
        // testTrip1 belongs to driver 1; driver 2 attempts to complete it
        String driver2Token = jwtService.generateToken(driver2);

        mockMvc.perform(post("/api/driver/trips/" + testTrip1.getId() + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Completing an already COMPLETED trip is idempotent")
    void testCompleteTrip_Idempotent() throws Exception {
        Trip completedTrip = new Trip(
                "TRIP-TEST-D1-COMP2",
                driver1,
                testLorry,
                owner,
                "Hyd",
                "Blr",
                TripStatus.COMPLETED,
                LocalDate.now(),
                Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(3600),
                new BigDecimal("500.00"),
                "Done"
        );
        completedTrip = tripRepository.save(completedTrip);

        mockMvc.perform(post("/api/driver/trips/" + completedTrip.getId() + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripStatus", is("COMPLETED")));
    }

    @Test
    @DisplayName("Owner cannot call driver start endpoint (returns 403 Forbidden)")
    void testStartTrip_OwnerRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/driver/trips/" + testTrip1.getId() + "/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to complete trip returns 401 Unauthorized")
    void testCompleteTrip_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/driver/trips/" + testTrip1.getId() + "/complete"))
                .andExpect(status().isUnauthorized());
    }
}
