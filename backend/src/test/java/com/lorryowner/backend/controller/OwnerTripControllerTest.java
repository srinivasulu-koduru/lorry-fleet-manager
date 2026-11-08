package com.lorryowner.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorryowner.backend.dto.trip.TripCreateDto;
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
import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OwnerTripControllerTest {

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
    private User otherOwnerDriver;
    private User inactiveDriver;

    private String ownerToken;
    private String otherOwnerToken;
    private String driverToken;

    private Lorry ownerLorry;
    private Lorry otherOwnerLorry;

    @BeforeEach
    void setUp() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        cleanupDatabase();

        // 1. Primary Owner
        owner = new User(
                "owner_trip_tester",
                passwordEncoder.encode("OwnerPass123!"),
                Role.OWNER,
                "Srinivasulu Owner",
                "9876543210",
                "owner_trip@fleet.test"
        );
        owner = userRepository.save(owner);
        ownerToken = jwtService.generateToken(owner);

        // 2. Rival Owner (for cross-owner IDOR security tests)
        otherOwner = new User(
                "other_owner_trip_tester",
                passwordEncoder.encode("OtherPass123!"),
                Role.OWNER,
                "Rival Owner",
                "9876543211",
                "rival@fleet.test"
        );
        otherOwner = userRepository.save(otherOwner);
        otherOwnerToken = jwtService.generateToken(otherOwner);

        // 3. Lorries
        ownerLorry = new Lorry("AP 31 TC 1234", "Tata Signa 4923", new BigDecimal("25.00"), OperationalStatus.AVAILABLE);
        ownerLorry.setOwner(owner);
        ownerLorry = lorryRepository.save(ownerLorry);

        otherOwnerLorry = new Lorry("TS 09 UA 9999", "Volvo FH 500", new BigDecimal("30.00"), OperationalStatus.AVAILABLE);
        otherOwnerLorry.setOwner(otherOwner);
        otherOwnerLorry = lorryRepository.save(otherOwnerLorry);

        // 4. Drivers
        driver = new User(
                "driver_ramesh_trip_test",
                passwordEncoder.encode("DriverPass123!"),
                Role.DRIVER,
                "Ramesh Kumar",
                "9876543210",
                "DL-TEST-001"
        );
        driver.setOwner(owner);
        driver.setActive(true);
        driver = userRepository.save(driver);
        driverToken = jwtService.generateToken(driver);

        otherOwnerDriver = new User(
                "other_driver_trip_test",
                passwordEncoder.encode("DriverPass123!"),
                Role.DRIVER,
                "Other Driver",
                "9876543212",
                "DL-TEST-002"
        );
        otherOwnerDriver.setOwner(otherOwner);
        otherOwnerDriver.setActive(true);
        otherOwnerDriver = userRepository.save(otherOwnerDriver);

        inactiveDriver = new User(
                "inactive_driver_test",
                passwordEncoder.encode("DriverPass123!"),
                Role.DRIVER,
                "Inactive Driver",
                "9876543213",
                "DL-TEST-003"
        );
        inactiveDriver.setOwner(owner);
        inactiveDriver.setActive(false);
        inactiveDriver = userRepository.save(inactiveDriver);
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        cleanupDatabase();
    }

    private void cleanupDatabase() {
        tripRepository.deleteAll();
        maintenanceRecordRepository.deleteAll();
        lorryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/owner/trips - Success with all required and optional fields")
    void createTrip_withAllFields_shouldReturn201() throws Exception {
        LocalDate depDate = LocalDate.now().plusDays(2);
        LocalTime depTime = LocalTime.of(9, 30);
        LocalDate delDate = LocalDate.now().plusDays(3);
        LocalTime delTime = LocalTime.of(18, 0);

        TripCreateDto request = new TripCreateDto(
                ownerLorry.getId(),
                driver.getId(),
                "Hyderabad Outer Ring",
                "Bangalore Electronic City",
                depDate,
                depTime,
                delDate,
                delTime,
                "General Goods",
                "Handle fragile electronics with extreme care."
        );

        mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.tripReference", startsWith("TRIP-")))
                .andExpect(jsonPath("$.lorryId", is(ownerLorry.getId().intValue())))
                .andExpect(jsonPath("$.lorryRegistration", is("AP 31 TC 1234")))
                .andExpect(jsonPath("$.driverId", is(driver.getId().intValue())))
                .andExpect(jsonPath("$.driverName", is("Ramesh Kumar")))
                .andExpect(jsonPath("$.origin", is("Hyderabad Outer Ring")))
                .andExpect(jsonPath("$.destination", is("Bangalore Electronic City")))
                .andExpect(jsonPath("$.plannedDepartureDate", is(depDate.toString())))
                .andExpect(jsonPath("$.plannedDeliveryDate", is(delDate.toString())))
                .andExpect(jsonPath("$.loadType", is("General Goods")))
                .andExpect(jsonPath("$.remarks", is("Handle fragile electronics with extreme care.")))
                .andExpect(jsonPath("$.status", is("PLANNED")));
    }

    @Test
    @DisplayName("POST /api/owner/trips - Success with optional delivery date/time omitted")
    void createTrip_withoutDeliveryDate_shouldReturn201WithNullDelivery() throws Exception {
        LocalDate depDate = LocalDate.now().plusDays(1);
        LocalTime depTime = LocalTime.of(10, 0);

        TripCreateDto request = new TripCreateDto(
                ownerLorry.getId(),
                driver.getId(),
                "Visakhapatnam Port",
                "Vijayawada Logistics Park",
                depDate,
                depTime,
                null,
                null,
                "Construction Materials",
                null
        );

        mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripReference", startsWith("TRIP-")))
                .andExpect(jsonPath("$.plannedDeliveryDate").doesNotExist())
                .andExpect(jsonPath("$.status", is("PLANNED")));
    }

    @Test
    @DisplayName("POST /api/owner/trips - Missing required fields should return 400 Bad Request")
    void createTrip_missingRequiredFields_shouldReturn400() throws Exception {
        TripCreateDto request = new TripCreateDto(
                null, // Missing lorryId
                driver.getId(),
                "", // Blank origin
                "Bangalore",
                null, // Missing departure date
                null, // Missing departure time
                null,
                null,
                "", // Blank loadType
                null
        );

        mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/owner/trips - Invalid Lorry ID should return 404 Not Found")
    void createTrip_invalidLorryId_shouldReturn404() throws Exception {
        TripCreateDto request = new TripCreateDto(
                999999L,
                driver.getId(),
                "Hyderabad",
                "Bangalore",
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                null,
                null,
                "General Goods",
                null
        );

        mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/owner/trips - Invalid Driver ID should return 404 Not Found")
    void createTrip_invalidDriverId_shouldReturn404() throws Exception {
        TripCreateDto request = new TripCreateDto(
                ownerLorry.getId(),
                999999L,
                "Hyderabad",
                "Bangalore",
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                null,
                null,
                "General Goods",
                null
        );

        mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/owner/trips - Cross-owner Lorry IDOR violation should return 403 Forbidden")
    void createTrip_crossOwnerLorry_shouldReturn403() throws Exception {
        TripCreateDto request = new TripCreateDto(
                otherOwnerLorry.getId(), // belongs to otherOwner
                driver.getId(),
                "Hyderabad",
                "Bangalore",
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                null,
                null,
                "General Goods",
                null
        );

        mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/owner/trips - Cross-owner Driver IDOR violation should return 403 Forbidden")
    void createTrip_crossOwnerDriver_shouldReturn403() throws Exception {
        TripCreateDto request = new TripCreateDto(
                ownerLorry.getId(),
                otherOwnerDriver.getId(), // belongs to otherOwner
                "Hyderabad",
                "Bangalore",
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                null,
                null,
                "General Goods",
                null
        );

        mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/owner/trips - Deactivated driver should return 400 Bad Request")
    void createTrip_deactivatedDriver_shouldReturn400() throws Exception {
        TripCreateDto request = new TripCreateDto(
                ownerLorry.getId(),
                inactiveDriver.getId(),
                "Hyderabad",
                "Bangalore",
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                null,
                null,
                "General Goods",
                null
        );

        mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/owner/trips - Delivery date present but time missing should return 400 Bad Request")
    void createTrip_unpairedDeliveryTimestamp_shouldReturn400() throws Exception {
        TripCreateDto request = new TripCreateDto(
                ownerLorry.getId(),
                driver.getId(),
                "Hyderabad",
                "Bangalore",
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                LocalDate.now().plusDays(2),
                null, // Missing delivery time
                "General Goods",
                null
        );

        mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/owner/trips - Delivery before departure should return 400 Bad Request")
    void createTrip_deliveryBeforeDeparture_shouldReturn400() throws Exception {
        TripCreateDto request = new TripCreateDto(
                ownerLorry.getId(),
                driver.getId(),
                "Hyderabad",
                "Bangalore",
                LocalDate.now().plusDays(2),
                LocalTime.of(12, 0),
                LocalDate.now().plusDays(1), // earlier date!
                LocalTime.of(10, 0),
                "General Goods",
                null
        );

        mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/owner/trips - Same origin and destination should return 400 Bad Request")
    void createTrip_sameOriginAndDestination_shouldReturn400() throws Exception {
        TripCreateDto request = new TripCreateDto(
                ownerLorry.getId(),
                driver.getId(),
                "Hyderabad",
                "Hyderabad",
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                null,
                null,
                "General Goods",
                null
        );

        mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/owner/trips - Driver role should return 403 Forbidden")
    void createTrip_asDriver_shouldReturn403() throws Exception {
        TripCreateDto request = new TripCreateDto(
                ownerLorry.getId(),
                driver.getId(),
                "Hyderabad",
                "Bangalore",
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                null,
                null,
                "General Goods",
                null
        );

        mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/owner/trips/{id} - Success and cross-owner security check")
    void getTripById_ownerAndCrossOwnerCheck() throws Exception {
        TripCreateDto request = new TripCreateDto(
                ownerLorry.getId(),
                driver.getId(),
                "Hyderabad",
                "Bangalore",
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                null,
                null,
                "General Goods",
                null
        );

        String responseJson = mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdTripId = objectMapper.readTree(responseJson).get("id").asLong();

        // 1. Primary owner can retrieve their trip
        mockMvc.perform(get("/api/owner/trips/" + createdTripId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdTripId.intValue())))
                .andExpect(jsonPath("$.origin", is("Hyderabad")));

        // 2. Rival owner cannot retrieve this trip (403 Forbidden)
        mockMvc.perform(get("/api/owner/trips/" + createdTripId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOwnerToken))
                .andExpect(status().isForbidden());
    }
}
