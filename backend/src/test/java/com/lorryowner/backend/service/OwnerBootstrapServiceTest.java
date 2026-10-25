package com.lorryowner.backend.service;

import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OwnerBootstrapServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private OwnerBootstrapService bootstrapService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        bootstrapService = new OwnerBootstrapService(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Creates initial Owner account when database is empty and credentials are provided")
    void run_whenEmptyDatabaseAndCredentialsProvided_createsOwnerAccount() {
        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "initial_boss");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "InitialSecret123!");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapFullName", "Boss Man");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPhoneNumber", "9876543210");

        bootstrapService.run(null);

        Optional<User> ownerOpt = userRepository.findByUsername("initial_boss");
        assertThat(ownerOpt).isPresent();
        User owner = ownerOpt.get();
        assertThat(owner.getRole()).isEqualTo(Role.OWNER);
        assertThat(owner.getFullName()).isEqualTo("Boss Man");
        assertThat(owner.getPhoneNumber()).isEqualTo("9876543210");
        assertThat(owner.isActive()).isTrue();
        // Verify password is BCrypt hashed and plaintext is not stored
        assertThat(owner.getPasswordHash()).isNotEqualTo("InitialSecret123!");
        assertThat(passwordEncoder.matches("InitialSecret123!", owner.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Refuses to create duplicate or overwrite when an Owner account already exists")
    void run_whenOwnerAlreadyExists_skipsBootstrap() {
        // Pre-create an existing owner
        User existing = new User(
                "existing_owner",
                passwordEncoder.encode("ExistingPass123!"),
                Role.OWNER,
                "Original Owner",
                null,
                null
        );
        userRepository.save(existing);

        // Attempt bootstrap with new credentials
        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "new_owner");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "NewPass123!");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapFullName", "New Boss");

        bootstrapService.run(null);

        // Verify new owner was NOT created
        assertThat(userRepository.findByUsername("new_owner")).isEmpty();
        // Verify existing owner is untouched
        assertThat(userRepository.findByUsername("existing_owner")).isPresent();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Skips owner creation safely without error when credentials are blank")
    void run_whenCredentialsMissing_skipsWithoutError() {
        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "");

        bootstrapService.run(null);

        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Aborts owner creation when password is shorter than 8 characters")
    void run_whenPasswordTooShort_abortsCreation() {
        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "short_pass_user");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "short");

        bootstrapService.run(null);

        assertThat(userRepository.findByUsername("short_pass_user")).isEmpty();
        assertThat(userRepository.count()).isEqualTo(0);
    }
}
