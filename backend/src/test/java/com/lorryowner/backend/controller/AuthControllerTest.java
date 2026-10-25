package com.lorryowner.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorryowner.backend.dto.auth.LoginRequest;
import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Seed an active Owner user
        User owner = new User(
                "owner1",
                passwordEncoder.encode("OwnerPass123!"),
                Role.OWNER,
                "Fleet Owner",
                "9876543210",
                "owner@example.com"
        );
        userRepository.save(owner);

        // Seed an active Driver user
        User driver = new User(
                "driver1",
                passwordEncoder.encode("DriverPass123!"),
                Role.DRIVER,
                "Ramesh Kumar",
                "9876543211",
                null
        );
        userRepository.save(driver);

        // Seed a disabled user
        User disabledUser = new User(
                "disabled_driver",
                passwordEncoder.encode("DisabledPass123!"),
                Role.DRIVER,
                "Inactive Driver",
                "9876543212",
                null
        );
        disabledUser.setActive(false);
        userRepository.save(disabledUser);
    }

    @Test
    @DisplayName("Owner login with valid credentials returns 200, JWT token, and OWNER role")
    void login_asOwner_withValidCredentials_shouldReturnJwtAndOwnerRole() throws Exception {
        LoginRequest request = new LoginRequest("owner1", "OwnerPass123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.username").value("owner1"))
                .andExpect(jsonPath("$.fullName").value("Fleet Owner"));
    }

    @Test
    @DisplayName("Driver login with valid credentials returns 200, JWT token, and DRIVER role")
    void login_asDriver_withValidCredentials_shouldReturnJwtAndDriverRole() throws Exception {
        LoginRequest request = new LoginRequest("driver1", "DriverPass123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.role").value("DRIVER"))
                .andExpect(jsonPath("$.username").value("driver1"))
                .andExpect(jsonPath("$.fullName").value("Ramesh Kumar"));
    }

    @Test
    @DisplayName("Login with invalid password returns 401 Unauthorized")
    void login_withInvalidPassword_shouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest("owner1", "WrongPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    @DisplayName("Login with non-existent username returns 401 Unauthorized")
    void login_withNonExistentUsername_shouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest("nonexistent_user", "AnyPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    @DisplayName("Login with disabled account returns 401 Unauthorized")
    void login_withDisabledAccount_shouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest("disabled_driver", "DisabledPass123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    @DisplayName("Login with blank credentials returns 400 Bad Request")
    void login_withBlankCredentials_shouldReturn400() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", notNullValue()));
    }
}
