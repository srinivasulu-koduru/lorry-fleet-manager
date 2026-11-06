package com.lorryowner.backend.service;

import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OwnerBootstrapServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    private OwnerBootstrapService bootstrapService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.execute("DELETE FROM app_settings");
            } catch (Exception ignored) {}
        }
        // Also clean up any test marker file
        File marker = new File("uploads/documents/.bootstrap_safeguard.properties");
        if (marker.exists()) {
            marker.delete();
        }

        bootstrapService = new OwnerBootstrapService(userRepository, passwordEncoder, jdbcTemplate);
    }

    // =========================================================================
    // INITIAL OWNER CREATION & PASSWORD RESET TESTS
    // =========================================================================

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
    @DisplayName("Skips owner creation safely without error when credentials are blank")
    void run_whenCredentialsMissing_skipsWithoutError() {
        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "");

        bootstrapService.run(null);

        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Aborts initial owner creation when password is shorter than 8 characters")
    void run_whenPasswordTooShort_abortsCreation() {
        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "short_pass_user");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "short");

        bootstrapService.run(null);

        assertThat(userRepository.findByUsername("short_pass_user")).isEmpty();
    }

    @Test
    @DisplayName("Skips reset when Owner already exists and OWNER_PASSWORD_RESET_ENABLED is false")
    void run_whenOwnerAlreadyExistsAndResetDisabled_skipsReset() {
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

        // Attempt bootstrap without reset enabled
        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "existing_owner");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "NewPass12345!");
        ReflectionTestUtils.setField(bootstrapService, "passwordResetEnabled", false);

        bootstrapService.run(null);

        // Verify password was NOT changed
        User fetched = userRepository.findByUsername("existing_owner").orElseThrow();
        assertThat(passwordEncoder.matches("ExistingPass123!", fetched.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("NewPass12345!", fetched.getPasswordHash())).isFalse();
    }

    @Test
    @DisplayName("Successfully resets Owner password when OWNER_PASSWORD_RESET_ENABLED is true")
    void run_whenOwnerAlreadyExistsAndResetEnabled_resetsOwnerPasswordSuccessfully() {
        // Pre-create existing owner
        User owner = new User(
                "bhp_owner",
                passwordEncoder.encode("OldPassword123!"),
                Role.OWNER,
                "BHP Logistics Owner",
                "9876543210",
                null
        );
        userRepository.save(owner);

        // Pre-create driver to verify drivers remain untouched
        User driver = new User(
                "driver_ramesh",
                passwordEncoder.encode("DriverSecret123!"),
                Role.DRIVER,
                "Ramesh Kumar",
                "9123456780",
                "DL-AP-12345"
        );
        driver.setOwner(owner);
        userRepository.save(driver);

        // Enable reset
        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "bhp_owner");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "NewResetPass123!");
        ReflectionTestUtils.setField(bootstrapService, "passwordResetEnabled", true);

        bootstrapService.run(null);

        // Verify owner password updated
        User updatedOwner = userRepository.findByUsername("bhp_owner").orElseThrow();
        assertThat(passwordEncoder.matches("NewResetPass123!", updatedOwner.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("OldPassword123!", updatedOwner.getPasswordHash())).isFalse();
        assertThat(updatedOwner.getPasswordUpdatedAt()).isNotNull();

        // Verify driver account remains untouched
        User untouchedDriver = userRepository.findByUsername("driver_ramesh").orElseThrow();
        assertThat(passwordEncoder.matches("DriverSecret123!", untouchedDriver.getPasswordHash())).isTrue();
        assertThat(untouchedDriver.getRole()).isEqualTo(Role.DRIVER);
    }

    @Test
    @DisplayName("Repeated startup safeguard prevents re-executing password reset on subsequent restarts")
    void run_whenResetExecuted_repeatedStartupSafeguardPreventsSubsequentReset() {
        User owner = new User(
                "bhp_owner",
                passwordEncoder.encode("OldPassword123!"),
                Role.OWNER,
                "BHP Logistics Owner",
                "9876543210",
                null
        );
        userRepository.save(owner);

        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "bhp_owner");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "ResetPassword123!");
        ReflectionTestUtils.setField(bootstrapService, "passwordResetEnabled", true);

        // 1. First execution: Reset runs successfully
        bootstrapService.run(null);

        User firstUpdated = userRepository.findByUsername("bhp_owner").orElseThrow();
        Instant firstUpdatedAt = firstUpdated.getPasswordUpdatedAt();
        assertThat(passwordEncoder.matches("ResetPassword123!", firstUpdated.getPasswordHash())).isTrue();

        // 2. Second execution (simulating container restart with same env vars)
        bootstrapService.run(null);

        User secondFetch = userRepository.findByUsername("bhp_owner").orElseThrow();
        // password_updated_at must remain identical, confirming no redundant write or token invalidation
        assertThat(secondFetch.getPasswordUpdatedAt()).isEqualTo(firstUpdatedAt);
        assertThat(passwordEncoder.matches("ResetPassword123!", secondFetch.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Safeguard protects new password chosen by Owner in-app across subsequent container restarts")
    void run_whenOwnerChangesPasswordInApp_repeatedStartupSafeguardProtectsNewPassword() {
        User owner = new User(
                "bhp_owner",
                passwordEncoder.encode("InitialOld123!"),
                Role.OWNER,
                "BHP Logistics Owner",
                null,
                null
        );
        userRepository.save(owner);

        // Step 1: Password reset through environment variable
        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "bhp_owner");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "EnvResetPassword123!");
        ReflectionTestUtils.setField(bootstrapService, "passwordResetEnabled", true);

        bootstrapService.run(null);

        // Step 2: Owner logs into mobile app and changes password to personal password
        User loggedInOwner = userRepository.findByUsername("bhp_owner").orElseThrow();
        loggedInOwner.setPasswordHash(passwordEncoder.encode("OwnerPersonalPass999!"));
        loggedInOwner.setPasswordUpdatedAt(Instant.now());
        userRepository.save(loggedInOwner);

        // Step 3: Container restarts while OWNER_PASSWORD_RESET_ENABLED=true is still present in Voroa
        bootstrapService.run(null);

        // Step 4: Verify the owner's personal password was NOT overwritten by the old env var
        User finalOwner = userRepository.findByUsername("bhp_owner").orElseThrow();
        assertThat(passwordEncoder.matches("OwnerPersonalPass999!", finalOwner.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("EnvResetPassword123!", finalOwner.getPasswordHash())).isFalse();
    }

    @Test
    @DisplayName("Aborts reset when target user is a DRIVER to protect driver accounts")
    void run_whenResetEnabled_butTargetUserIsNotOwner_abortsWithoutModifyingDriver() {
        User driver = new User(
                "driver_john",
                passwordEncoder.encode("DriverPass123!"),
                Role.DRIVER,
                "John Driver",
                null,
                "DL-123"
        );
        userRepository.save(driver);

        // Also add an owner so ownerExists is true
        User owner = new User("real_owner", passwordEncoder.encode("RealOwner123!"), Role.OWNER, "Owner", null, null);
        userRepository.save(owner);

        // Attempt reset targeting the driver
        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "driver_john");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "NewHackPass123!");
        ReflectionTestUtils.setField(bootstrapService, "passwordResetEnabled", true);

        bootstrapService.run(null);

        // Verify driver password was NOT touched
        User fetchedDriver = userRepository.findByUsername("driver_john").orElseThrow();
        assertThat(passwordEncoder.matches("DriverPass123!", fetchedDriver.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("NewHackPass123!", fetchedDriver.getPasswordHash())).isFalse();
    }

    @Test
    @DisplayName("Aborts reset safely when username does not exist")
    void run_whenResetEnabled_butUsernameNotFound_abortsWithoutModifyingExistingAccounts() {
        User owner = new User("real_owner", passwordEncoder.encode("RealOwner123!"), Role.OWNER, "Owner", null, null);
        userRepository.save(owner);

        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "non_existent_user");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "NewPassword123!");
        ReflectionTestUtils.setField(bootstrapService, "passwordResetEnabled", true);

        bootstrapService.run(null);

        User fetchedOwner = userRepository.findByUsername("real_owner").orElseThrow();
        assertThat(passwordEncoder.matches("RealOwner123!", fetchedOwner.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Aborts reset when new password is shorter than 8 characters")
    void run_whenResetEnabled_butPasswordTooShort_abortsReset() {
        User owner = new User("bhp_owner", passwordEncoder.encode("ValidOldPass123!"), Role.OWNER, "Owner", null, null);
        userRepository.save(owner);

        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "bhp_owner");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "short");
        ReflectionTestUtils.setField(bootstrapService, "passwordResetEnabled", true);

        bootstrapService.run(null);

        User fetchedOwner = userRepository.findByUsername("bhp_owner").orElseThrow();
        assertThat(passwordEncoder.matches("ValidOldPass123!", fetchedOwner.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Executes a new reset when a distinct password is intentionally provided later")
    void run_whenResetEnabled_withDifferentNewPassword_executesNewResetSuccessfully() {
        User owner = new User("bhp_owner", passwordEncoder.encode("OldPass123!"), Role.OWNER, "Owner", null, null);
        userRepository.save(owner);

        // First reset
        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "bhp_owner");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "FirstResetPass123!");
        ReflectionTestUtils.setField(bootstrapService, "passwordResetEnabled", true);
        bootstrapService.run(null);

        User afterFirst = userRepository.findByUsername("bhp_owner").orElseThrow();
        assertThat(passwordEncoder.matches("FirstResetPass123!", afterFirst.getPasswordHash())).isTrue();

        // Distinct new password provided in future
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "SecondDistinctPass456!");
        bootstrapService.run(null);

        User afterSecond = userRepository.findByUsername("bhp_owner").orElseThrow();
        assertThat(passwordEncoder.matches("SecondDistinctPass456!", afterSecond.getPasswordHash())).isTrue();
    }

    // =========================================================================
    // SECOND OWNER PROVISIONING TESTS
    // =========================================================================

    @Test
    @DisplayName("Successfully creates second Owner account when credentials are provided")
    void run_whenSecondOwnerCredentialsProvided_createsSecondOwnerSuccessfully() {
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerUsername", "owner_partner");
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerPassword", "PartnerPass123!");
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerFullName", "Managing Partner");
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerPhoneNumber", "9876543222");

        bootstrapService.run(null);

        Optional<User> secondOwnerOpt = userRepository.findByUsername("owner_partner");
        assertThat(secondOwnerOpt).isPresent();
        User secondOwner = secondOwnerOpt.get();
        assertThat(secondOwner.getRole()).isEqualTo(Role.OWNER);
        assertThat(secondOwner.getFullName()).isEqualTo("Managing Partner");
        assertThat(secondOwner.getPhoneNumber()).isEqualTo("9876543222");
        assertThat(secondOwner.isActive()).isTrue();
        assertThat(secondOwner.getPasswordHash()).isNotEqualTo("PartnerPass123!");
        assertThat(passwordEncoder.matches("PartnerPass123!", secondOwner.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Creates both initial and second owner accounts when both are provided on clean database")
    void run_whenBothInitialAndSecondOwnerProvided_createsBothAccountsSuccessfully() {
        // Initial Owner
        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "bhp_owner");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "InitialPass123!");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapFullName", "Primary Owner");

        // Second Owner
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerUsername", "second_owner");
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerPassword", "SecondPass123!");
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerFullName", "Co-Owner");

        bootstrapService.run(null);

        assertThat(userRepository.count()).isEqualTo(2);

        User primary = userRepository.findByUsername("bhp_owner").orElseThrow();
        assertThat(primary.getRole()).isEqualTo(Role.OWNER);
        assertThat(passwordEncoder.matches("InitialPass123!", primary.getPasswordHash())).isTrue();

        User secondary = userRepository.findByUsername("second_owner").orElseThrow();
        assertThat(secondary.getRole()).isEqualTo(Role.OWNER);
        assertThat(passwordEncoder.matches("SecondPass123!", secondary.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Skips second owner creation when username or password is blank or missing")
    void run_whenSecondOwnerCredentialsMissing_skipsSecondOwnerCreation() {
        // 1. Username provided, password blank
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerUsername", "second_owner");
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerPassword", "");
        bootstrapService.run(null);
        assertThat(userRepository.findByUsername("second_owner")).isEmpty();

        // 2. Password provided, username blank
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerUsername", "");
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerPassword", "SecondPass123!");
        bootstrapService.run(null);
        assertThat(userRepository.count()).isEqualTo(0);

        // 3. Both null/empty
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerUsername", null);
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerPassword", null);
        bootstrapService.run(null);
        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Aborts second owner creation when password is shorter than 8 characters")
    void run_whenSecondOwnerPasswordTooShort_abortsSecondOwnerCreation() {
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerUsername", "short_pass_second");
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerPassword", "short");

        bootstrapService.run(null);

        assertThat(userRepository.findByUsername("short_pass_second")).isEmpty();
    }

    @Test
    @DisplayName("Skips creation and never modifies existing account when second owner username already exists")
    void run_whenSecondOwnerUsernameAlreadyExists_skipsCreationWithoutModifyingExisting() {
        // Pre-create an existing user with username "co_owner"
        User existing = new User(
                "co_owner",
                passwordEncoder.encode("OriginalPass123!"),
                Role.OWNER,
                "Original Co-Owner",
                "9998887776",
                null
        );
        userRepository.save(existing);

        // Attempt second owner creation with same username but different password
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerUsername", "co_owner");
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerPassword", "NewHackerPassword123!");
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerFullName", "Imposter");

        bootstrapService.run(null);

        // Verify account details and password remain unchanged
        User fetched = userRepository.findByUsername("co_owner").orElseThrow();
        assertThat(passwordEncoder.matches("OriginalPass123!", fetched.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("NewHackerPassword123!", fetched.getPasswordHash())).isFalse();
        assertThat(fetched.getFullName()).isEqualTo("Original Co-Owner");
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Repeated startup does not duplicate or modify second owner account")
    void run_repeatedStartup_doesNotDuplicateOrModifySecondOwner() {
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerUsername", "second_owner");
        ReflectionTestUtils.setField(bootstrapService, "secondOwnerPassword", "SecondPass123!");

        // 1. First run creates the account
        bootstrapService.run(null);
        assertThat(userRepository.count()).isEqualTo(1);

        User firstCreated = userRepository.findByUsername("second_owner").orElseThrow();
        Instant firstUpdatedAt = firstCreated.getUpdatedAt();

        // 2. Second run (simulating application restart)
        bootstrapService.run(null);

        // Total count must remain 1
        assertThat(userRepository.count()).isEqualTo(1);
        User secondFetch = userRepository.findByUsername("second_owner").orElseThrow();
        assertThat(secondFetch.getUpdatedAt()).isEqualTo(firstUpdatedAt);
        assertThat(passwordEncoder.matches("SecondPass123!", secondFetch.getPasswordHash())).isTrue();
    }
}
