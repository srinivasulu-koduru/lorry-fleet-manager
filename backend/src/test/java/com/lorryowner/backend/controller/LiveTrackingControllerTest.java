package com.lorryowner.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorryowner.backend.dto.tracking.SubmitLocationRequest;
import com.lorryowner.backend.entity.*;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.TripLocationRepository;
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
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LiveTrackingControllerTest {

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
    private TripLocationRepository tripLocationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User owner1;
    private User owner2;
    private User driver1;
    private User driver2;
    private Lorry lorry1;
    private Lorry lorry2;
    private Trip tripActive;
    private Trip tripCompleted;
    private Trip tripPlanned;
    private String owner1Token;
    private String owner2Token;
    private String driver1Token;
    private String driver2Token;

    @BeforeEach
    void setUp() {
        tearDown();

        owner1 = new User("track_owner1", passwordEncoder.encode("Pass123!"), Role.OWNER, "Owner One", "9876543111", null);
        owner1 = userRepository.save(owner1);

        owner2 = new User("track_owner2", passwordEncoder.encode("Pass123!"), Role.OWNER, "Owner Two", "9876543112", null);
        owner2 = userRepository.save(owner2);

        driver1 = new User("track_driver1", passwordEncoder.encode("Pass123!"), Role.DRIVER, "Driver One", "9876543221", "DL-12345");
        driver1 = userRepository.save(driver1);

        driver2 = new User("track_driver2", passwordEncoder.encode("Pass123!"), Role.DRIVER, "Driver Two", "9876543222", "DL-67890");
        driver2 = userRepository.save(driver2);

        lorry1 = new Lorry("AP 31 TR 0001", "Tata Prima 5530.S", BigDecimal.valueOf(35.0), OperationalStatus.ON_TRIP);
        lorry1.setOwner(owner1);
        lorry1 = lorryRepository.save(lorry1);

        lorry2 = new Lorry("AP 31 TR 0002", "Ashok Leyland 4220", BigDecimal.valueOf(40.0), OperationalStatus.AVAILABLE);
        lorry2.setOwner(owner2);
        lorry2 = lorryRepository.save(lorry2);

        tripActive = new Trip(
                "TRIP-TRACK-001",
                driver1,
                lorry1,
                owner1,
                "Hyderabad Logistics Park, TS",
                "Nellore Distribution Center, AP",
                TripStatus.IN_PROGRESS,
                LocalDate.now(),
                Instant.now().minus(2, ChronoUnit.HOURS),
                null,
                BigDecimal.valueOf(450.0),
                "Live tracking test active trip"
        );
        tripActive = tripRepository.save(tripActive);

        tripCompleted = new Trip(
                "TRIP-TRACK-002",
                driver1,
                lorry1,
                owner1,
                "Vijayawada",
                "Visakhapatnam",
                TripStatus.COMPLETED,
                LocalDate.now().minusDays(1),
                Instant.now().minus(24, ChronoUnit.HOURS),
                Instant.now().minus(16, ChronoUnit.HOURS),
                BigDecimal.valueOf(350.0),
                "Completed trip"
        );
        tripCompleted = tripRepository.save(tripCompleted);

        tripPlanned = new Trip(
                "TRIP-TRACK-003",
                driver1,
                lorry1,
                owner1,
                "Chennai",
                "Bengaluru",
                TripStatus.PLANNED,
                LocalDate.now().plusDays(1),
                null,
                null,
                BigDecimal.valueOf(330.0),
                "Planned trip"
        );
        tripPlanned = tripRepository.save(tripPlanned);

        owner1Token = jwtService.generateToken(owner1);
        owner2Token = jwtService.generateToken(owner2);
        driver1Token = jwtService.generateToken(driver1);
        driver2Token = jwtService.generateToken(driver2);
    }

    @AfterEach
    void tearDown() {
        tripLocationRepository.deleteAll();
        tripRepository.deleteAll();
        lorryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Driver successfully submits live GPS location for assigned in-progress trip")
    void testDriverSubmitsValidLocationSuccess() throws Exception {
        SubmitLocationRequest request = new SubmitLocationRequest(
                15.8281,
                78.0373,
                58.0,
                165.5,
                4.5,
                Instant.now()
        );

        mockMvc.perform(post("/api/driver/trips/{tripId}/location", tripActive.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId", is(tripActive.getId().intValue())))
                .andExpect(jsonPath("$.registrationNumber", is("AP 31 TR 0001")))
                .andExpect(jsonPath("$.currentSpeedKmh", is(58.0)))
                .andExpect(jsonPath("$.gpsStatus", is("LIVE")))
                .andExpect(jsonPath("$.isLive", is(true)))
                .andExpect(jsonPath("$.hasGpsFix", is(true)));
    }

    @Test
    @DisplayName("Driver cannot submit location for another driver's trip (Forbidden 403)")
    void testDriverSubmitsLocationForAnotherDriversTripForbidden() throws Exception {
        SubmitLocationRequest request = new SubmitLocationRequest(
                15.8281,
                78.0373,
                55.0,
                null,
                null,
                Instant.now()
        );

        mockMvc.perform(post("/api/driver/trips/{tripId}/location", tripActive.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver2Token) // Driver 2 is not assigned
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Driver cannot submit location for a completed trip (Bad Request 400)")
    void testDriverSubmitsLocationForCompletedTripBadRequest() throws Exception {
        SubmitLocationRequest request = new SubmitLocationRequest(
                15.8281,
                78.0373,
                55.0,
                null,
                null,
                Instant.now()
        );

        mockMvc.perform(post("/api/driver/trips/{tripId}/location", tripCompleted.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Driver cannot submit location for a planned trip (Bad Request 400)")
    void testDriverSubmitsLocationForPlannedTripBadRequest() throws Exception {
        SubmitLocationRequest request = new SubmitLocationRequest(
                15.8281,
                78.0373,
                55.0,
                null,
                null,
                Instant.now()
        );

        mockMvc.perform(post("/api/driver/trips/{tripId}/location", tripPlanned.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Rejects invalid coordinates (lat > 90.0 or lon > 180.0)")
    void testDriverSubmitsInvalidCoordinatesBadRequest() throws Exception {
        SubmitLocationRequest invalidLat = new SubmitLocationRequest(
                95.0, // invalid latitude > 90
                78.0373,
                55.0,
                null,
                null,
                Instant.now()
        );

        mockMvc.perform(post("/api/driver/trips/{tripId}/location", tripActive.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLat)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Rejects negative speed (Bad Request 400)")
    void testDriverSubmitsNegativeSpeedBadRequest() throws Exception {
        SubmitLocationRequest negativeSpeed = new SubmitLocationRequest(
                15.8281,
                78.0373,
                -25.0, // invalid negative speed
                null,
                null,
                Instant.now()
        );

        mockMvc.perform(post("/api/driver/trips/{tripId}/location", tripActive.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeSpeed)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Owner successfully fetches running lorries with active trip")
    void testOwnerGetsRunningLorriesSuccess() throws Exception {
        // Submit one location first
        SubmitLocationRequest request = new SubmitLocationRequest(
                15.8281,
                78.0373,
                58.0,
                165.5,
                4.5,
                Instant.now()
        );
        mockMvc.perform(post("/api/driver/trips/{tripId}/location", tripActive.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/owner/tracking/running-lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lorryId", is(lorry1.getId().intValue())))
                .andExpect(jsonPath("$[0].registrationNumber", is("AP 31 TR 0001")))
                .andExpect(jsonPath("$[0].driverName", is("Driver One")))
                .andExpect(jsonPath("$[0].currentSpeedKmh", is(58.0)))
                .andExpect(jsonPath("$[0].gpsStatus", is("LIVE")))
                .andExpect(jsonPath("$[0].isLive", is(true)));
    }

    @Test
    @DisplayName("Owner gets detailed trip live tracking with breadcrumbs")
    void testOwnerGetsTripTrackingDetailsSuccess() throws Exception {
        // Submit location
        SubmitLocationRequest request = new SubmitLocationRequest(
                15.8281,
                78.0373,
                58.0,
                165.5,
                4.5,
                Instant.now()
        );
        mockMvc.perform(post("/api/driver/trips/{tripId}/location", tripActive.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/owner/tracking/trips/{tripId}", tripActive.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId", is(tripActive.getId().intValue())))
                .andExpect(jsonPath("$.registrationNumber", is("AP 31 TR 0001")))
                .andExpect(jsonPath("$.latitude", is(15.8281)))
                .andExpect(jsonPath("$.longitude", is(78.0373)))
                .andExpect(jsonPath("$.breadcrumbs", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Cross-owner access is forbidden (403)")
    void testCrossOwnerAccessDeniedForbidden() throws Exception {
        // Owner 2 attempts to track Owner 1's trip
        mockMvc.perform(get("/api/owner/tracking/trips/{tripId}", tripActive.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Driver role cannot access owner tracking endpoints (403)")
    void testDriverAccessingOwnerTrackingEndpointsForbidden() throws Exception {
        mockMvc.perform(get("/api/owner/tracking/running-lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated requests return 401 Unauthorized")
    void testUnauthenticatedRequestsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/owner/tracking/running-lorries"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/driver/trips/{tripId}/location", tripActive.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
