package com.lorryowner.backend.service;

import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Secure Initial Owner Account Provisioning and One-Time Password Reset Service.
 *
 * Requirements enforced:
 * 1. Supports INITIAL_OWNER_USERNAME and INITIAL_OWNER_PASSWORD environment variables.
 * 2. If no OWNER exists in the database, provisions the initial Owner account.
 * 3. If an OWNER already exists, does not silently skip; supports an explicitly enabled,
 *    safe one-time password reset when OWNER_PASSWORD_RESET_ENABLED=true.
 * 4. Password reset updates only the existing OWNER account matching INITIAL_OWNER_USERNAME,
 *    hashes credentials with BCrypt, refreshes password_updated_at, and leaves all driver
 *    accounts and other records untouched.
 * 5. When password reset succeeds, a persistent safeguard (dual-layer: database app_settings table
 *    and upload directory marker) guarantees that subsequent application restarts cannot
 *    accidentally reset or overwrite the owner password repeatedly.
 * 6. Never logs plaintext passwords or exposes sensitive credentials through any API.
 */
@Service
public class OwnerBootstrapService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OwnerBootstrapService.class);
    private static final String RESET_SETTING_PREFIX = "owner_password_reset:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${bootstrap.owner.username:}")
    private String bootstrapUsername;

    @Value("${bootstrap.owner.password:}")
    private String bootstrapPassword;

    @Value("${bootstrap.owner.full-name:Fleet Owner}")
    private String bootstrapFullName;

    @Value("${bootstrap.owner.phone-number:}")
    private String bootstrapPhoneNumber;

    @Value("${bootstrap.owner.password-reset-enabled:false}")
    private boolean passwordResetEnabled;

    @Value("${app.document.upload-dir:uploads/documents}")
    private String uploadDir;

    @Autowired
    public OwnerBootstrapService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Autowired(required = false) JdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    public OwnerBootstrapService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, null);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureSafeguardTableExists();

        boolean ownerExists = userRepository.existsByRole(Role.OWNER);

        if (!ownerExists) {
            handleInitialOwnerCreation();
        } else {
            handleExistingOwnerScenario();
        }
    }

    /**
     * Provisions the initial Owner account when no OWNER exists in the database.
     */
    private void handleInitialOwnerCreation() {
        String username = bootstrapUsername != null ? bootstrapUsername.trim() : "";
        String password = bootstrapPassword != null ? bootstrapPassword.trim() : "";

        if (username.isEmpty() || password.isEmpty()) {
            log.warn("================================================================================");
            log.warn("INITIAL OWNER SETUP NOTICE:");
            log.warn("No Owner account exists in the database.");
            log.warn("Initial owner creation skipped because INITIAL_OWNER_USERNAME or INITIAL_OWNER_PASSWORD");
            log.warn("environment variables are not set. Set these variables to provision your initial Owner.");
            log.warn("================================================================================");
            return;
        }

        if (password.length() < 8) {
            log.error("INITIAL_OWNER_PASSWORD must be at least 8 characters long. Initial owner creation aborted.");
            return;
        }

        User owner = new User(
                username,
                passwordEncoder.encode(password),
                Role.OWNER,
                bootstrapFullName.trim().isEmpty() ? "Fleet Owner" : bootstrapFullName.trim(),
                bootstrapPhoneNumber.trim().isEmpty() ? null : bootstrapPhoneNumber.trim(),
                null
        );

        userRepository.save(owner);
        recordSafeguard(username, password);

        log.info("================================================================================");
        log.info("INITIAL OWNER ACCOUNT PROVISIONED SUCCESSFULLY:");
        log.info("Username: '{}' | Role: OWNER", username);
        log.info("Password was hashed with BCrypt. Plaintext credentials were not stored or logged.");
        log.info("================================================================================");
    }

    /**
     * Handles startup when an Owner account already exists.
     * If OWNER_PASSWORD_RESET_ENABLED=true, performs a safe one-time password reset for INITIAL_OWNER_USERNAME.
     */
    private void handleExistingOwnerScenario() {
        String username = bootstrapUsername != null ? bootstrapUsername.trim() : "";
        String password = bootstrapPassword != null ? bootstrapPassword.trim() : "";

        if (!passwordResetEnabled) {
            log.info("Owner account already provisioned in database. Skipping initial owner bootstrap. " +
                     "To securely reset the owner password, set OWNER_PASSWORD_RESET_ENABLED=true along with " +
                     "INITIAL_OWNER_USERNAME and INITIAL_OWNER_PASSWORD.");
            return;
        }

        log.info("OWNER_PASSWORD_RESET_ENABLED is true. Checking one-time owner password reset prerequisites...");

        if (username.isEmpty() || password.isEmpty()) {
            log.warn("Owner password reset was requested (OWNER_PASSWORD_RESET_ENABLED=true), but " +
                     "INITIAL_OWNER_USERNAME or INITIAL_OWNER_PASSWORD is empty. No changes made.");
            return;
        }

        if (password.length() < 8) {
            log.error("INITIAL_OWNER_PASSWORD must be at least 8 characters long for password reset. Reset aborted.");
            return;
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("Owner password reset aborted: No account found with username '{}'. Existing accounts were not modified.", username);
            return;
        }

        User owner = userOpt.get();
        if (owner.getRole() != Role.OWNER) {
            log.warn("Owner password reset aborted: User '{}' does not have role OWNER (found role: {}). " +
                     "Non-owner and driver accounts cannot be modified via bootstrap reset.", username, owner.getRole());
            return;
        }

        // Persistent one-time safeguard check to prevent repeated reset across restarts
        if (hasResetBeenExecuted(username, password)) {
            log.info("Owner password reset for '{}' has already been executed previously. " +
                     "Persistent safeguard prevented repeated execution on application restart. " +
                     "Please set OWNER_PASSWORD_RESET_ENABLED=false in Voroa environment variables.", username);
            return;
        }

        // Apply BCrypt password update and refresh password_updated_at
        owner.setPasswordHash(passwordEncoder.encode(password));
        owner.setPasswordUpdatedAt(Instant.now());
        userRepository.save(owner);

        // Record persistent safeguard
        recordSafeguard(username, password);

        log.info("================================================================================");
        log.info("OWNER PASSWORD RESET EXECUTED SUCCESSFULLY:");
        log.info("Owner username: '{}'", username);
        log.info("Password hashed with BCrypt. password_updated_at refreshed (invalidates older JWTs).");
        log.info("Persistent safeguard recorded. Repeated execution on future restarts is blocked.");
        log.info("Action Required: Set OWNER_PASSWORD_RESET_ENABLED=false in Voroa environment variables.");
        log.info("================================================================================");
    }

    /**
     * Ensures the app_settings table exists in the database for storing persistent safeguards.
     */
    private void ensureSafeguardTableExists() {
        if (jdbcTemplate == null) {
            return;
        }
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS app_settings (" +
                    "setting_key VARCHAR(100) PRIMARY KEY, " +
                    "setting_value TEXT NOT NULL, " +
                    "updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP" +
                    ")"
            );
        } catch (Exception ex) {
            log.warn("Could not ensure app_settings table exists via JDBC (falling back to file safeguard): {}", ex.getMessage());
        }
    }

    /**
     * Checks if this password reset has already been executed.
     */
    private boolean hasResetBeenExecuted(String username, String password) {
        String key = RESET_SETTING_PREFIX + username;
        String expectedFingerprint = computeFingerprint(username, password);

        // 1. Check database safeguard
        if (jdbcTemplate != null) {
            try {
                List<String> results = jdbcTemplate.query(
                        "SELECT setting_value FROM app_settings WHERE setting_key = ?",
                        (rs, rowNum) -> rs.getString("setting_value"),
                        key
                );
                if (!results.isEmpty() && expectedFingerprint.equals(results.get(0))) {
                    return true;
                }
            } catch (Exception ex) {
                log.warn("Error querying database safeguard: {}", ex.getMessage());
            }
        }

        // 2. Check file fallback safeguard
        return checkFileSafeguard(key, expectedFingerprint);
    }

    /**
     * Records the executed password reset fingerprint persistently.
     */
    private void recordSafeguard(String username, String password) {
        String key = RESET_SETTING_PREFIX + username;
        String fingerprint = computeFingerprint(username, password);

        // 1. Record in database safeguard
        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.update("DELETE FROM app_settings WHERE setting_key = ?", key);
                jdbcTemplate.update(
                        "INSERT INTO app_settings (setting_key, setting_value, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                        key, fingerprint
                );
            } catch (Exception ex) {
                log.warn("Error saving database safeguard: {}", ex.getMessage());
            }
        }

        // 2. Record in file fallback safeguard
        saveFileSafeguard(key, fingerprint);
    }

    /**
     * Computes a one-way SHA-256 fingerprint from credentials to verify execution without storing plaintext.
     */
    private String computeFingerprint(String username, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private boolean checkFileSafeguard(String key, String expectedFingerprint) {
        try {
            File markerFile = getMarkerFile();
            if (markerFile.exists()) {
                Properties props = new Properties();
                try (InputStream in = new FileInputStream(markerFile)) {
                    props.load(in);
                }
                return expectedFingerprint.equals(props.getProperty(key));
            }
        } catch (Exception ex) {
            log.debug("Error checking file safeguard: {}", ex.getMessage());
        }
        return false;
    }

    private void saveFileSafeguard(String key, String fingerprint) {
        try {
            File markerFile = getMarkerFile();
            File parentDir = markerFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            Properties props = new Properties();
            if (markerFile.exists()) {
                try (InputStream in = new FileInputStream(markerFile)) {
                    props.load(in);
                }
            }
            props.setProperty(key, fingerprint);
            try (OutputStream out = new FileOutputStream(markerFile)) {
                props.store(out, "BHP Logistics Safeguard Properties");
            }
        } catch (Exception ex) {
            log.debug("Error saving file safeguard: {}", ex.getMessage());
        }
    }

    private File getMarkerFile() {
        return new File(uploadDir, ".bootstrap_safeguard.properties");
    }
}
