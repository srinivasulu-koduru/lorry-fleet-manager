package com.lorryowner.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorryowner.backend.dto.user.ChangePasswordRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.lorryowner.backend.repository.LorryRepository lorryRepository;

    @Autowired
    private com.lorryowner.backend.repository.TripRepository tripRepository;

    @Autowired
    private com.lorryowner.backend.repository.MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        maintenanceRecordRepository.deleteAll();
        tripRepository.deleteAll();
        lorryRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User(
                "testuser",
                passwordEncoder.encode("CurrentPass123!"),
                Role.OWNER,
                "Test Fleet Owner",
                "9876543210",
                "test@example.com"
        );
        testUser = userRepository.save(testUser);

        jwtToken = jwtService.generateToken(testUser);
    }

    @Test
    @DisplayName("GET /api/users/me with valid JWT returns current user profile")
    void getCurrentUser_withValidJwt_shouldReturnProfile() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.fullName").value("Test Fleet Owner"))
                .andExpect(jsonPath("$.phoneNumber").value("9876543210"));
    }

    @Test
    @DisplayName("GET /api/users/me unauthenticated returns 401 Unauthorized")
    void getCurrentUser_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/users/change-password with valid current password succeeds and updates password")
    void changePassword_withValidCurrentPassword_shouldSucceed() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass123!",
                "NewSecurePassword123!",
                "NewSecurePassword123!"
        );

        mockMvc.perform(post("/api/users/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully. Existing session tokens have been invalidated."));

        // Verify password in DB has been changed
        User updated = userRepository.findByUsername("testuser").orElseThrow();
        assertThat(passwordEncoder.matches("NewSecurePassword123!", updated.getPasswordHash())).isTrue();
        assertThat(updated.getPasswordUpdatedAt()).isNotNull();

        // Verify that the old token is now rejected because token issuedAt is before passwordUpdatedAt
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/users/change-password with incorrect current password returns 400 Bad Request")
    void changePassword_withIncorrectCurrentPassword_shouldReturn400() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "WrongCurrentPass!",
                "NewSecurePassword123!",
                "NewSecurePassword123!"
        );

        mockMvc.perform(post("/api/users/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password does not match."));
    }

    @Test
    @DisplayName("POST /api/users/change-password with short new password returns 400 Bad Request")
    void changePassword_withShortNewPassword_shouldReturn400() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass123!",
                "short",
                "short"
        );

        mockMvc.perform(post("/api/users/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/owner/change-password with valid credentials succeeds via owner route")
    void changePassword_viaOwnerRoute_shouldSucceed() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass123!",
                "BrandNewSecurePass99!",
                "BrandNewSecurePass99!"
        );

        mockMvc.perform(post("/api/owner/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully. Existing session tokens have been invalidated."));
    }

    @Test
    @DisplayName("GET /api/owner/me with valid JWT returns current owner profile")
    void getCurrentUser_viaOwnerRoute_shouldReturnProfile() throws Exception {
        mockMvc.perform(get("/api/owner/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        maintenanceRecordRepository.deleteAll();
        tripRepository.deleteAll();
        lorryRepository.deleteAll();
        userRepository.deleteAll();
    }
}
