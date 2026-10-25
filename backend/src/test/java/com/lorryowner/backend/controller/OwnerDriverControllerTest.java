package com.lorryowner.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorryowner.backend.dto.driver.CreateDriverRequest;
import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.UserRepository;
import com.lorryowner.backend.security.JwtService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String ownerToken;
    private String driverToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Create Owner
        User owner = new User(
                "fleet_boss",
                passwordEncoder.encode("BossPassword123!"),
                Role.OWNER,
                "Fleet Boss",
                "9876543210",
                "boss@example.com"
        );
        owner = userRepository.save(owner);
        ownerToken = jwtService.generateToken(owner);

        // Create Driver
        User driver = new User(
                "driver_john",
                passwordEncoder.encode("DriverPass123!"),
                Role.DRIVER,
                "John Doe",
                "9876543211",
                null
        );
        driver = userRepository.save(driver);
        driverToken = jwtService.generateToken(driver);
    }

    @Test
    @DisplayName("Owner can create a new permanent Driver account")
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
    }

    @Test
    @DisplayName("Owner can list all registered drivers")
    void getAllDrivers_asOwner_shouldReturnDriverList() throws Exception {
        mockMvc.perform(get("/api/owner/drivers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("driver_john"))
                .andExpect(jsonPath("$[0].fullName").value("John Doe"));
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
    @DisplayName("Creating driver with duplicate username returns 400 Bad Request")
    void createDriver_withDuplicateUsername_shouldReturn400() throws Exception {
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
}
