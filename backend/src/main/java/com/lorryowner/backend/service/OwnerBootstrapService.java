package com.lorryowner.backend.service;

import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secure one-time initial Owner account bootstrap service.
 *
 * Requirements enforced:
 * 1. Checks if an account with role OWNER already exists; if so, refuses to overwrite.
 * 2. Reads initial credentials strictly from environment configuration (INITIAL_OWNER_USERNAME, INITIAL_OWNER_PASSWORD).
 * 3. Never logs plaintext passwords or tokens.
 * 4. Never creates default/guessed passwords (like admin/admin) if environment variables are missing.
 * 5. Fails safely with an explanatory message if configuration is absent.
 */
@Service
public class OwnerBootstrapService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OwnerBootstrapService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bootstrap.owner.username:}")
    private String bootstrapUsername;

    @Value("${bootstrap.owner.password:}")
    private String bootstrapPassword;

    @Value("${bootstrap.owner.full-name:Fleet Owner}")
    private String bootstrapFullName;

    @Value("${bootstrap.owner.phone-number:}")
    private String bootstrapPhoneNumber;

    public OwnerBootstrapService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 1. Check if an Owner account already exists in the database
        if (userRepository.existsByRole(Role.OWNER)) {
            log.info("Owner account already provisioned in database. Skipping initial owner bootstrap.");
            return;
        }

        // 2. Validate environment configuration
        String username = bootstrapUsername != null ? bootstrapUsername.trim() : "";
        String password = bootstrapPassword != null ? bootstrapPassword.trim() : "";

        if (username.isEmpty() || password.isEmpty()) {
            log.warn("================================================================================");
            log.warn("INITIAL OWNER SETUP NOTICE:");
            log.warn("No Owner account exists in lorry_fleet_db.");
            log.warn("Initial owner creation skipped because INITIAL_OWNER_USERNAME or INITIAL_OWNER_PASSWORD");
            log.warn("environment variables are not set. Set these variables to provision your initial Owner.");
            log.warn("================================================================================");
            return;
        }

        if (password.length() < 8) {
            log.error("INITIAL_OWNER_PASSWORD must be at least 8 characters long. Owner creation aborted.");
            return;
        }

        // 3. Create initial Owner account with BCrypt password hashing
        User owner = new User(
                username,
                passwordEncoder.encode(password),
                Role.OWNER,
                bootstrapFullName.trim().isEmpty() ? "Fleet Owner" : bootstrapFullName.trim(),
                bootstrapPhoneNumber.trim().isEmpty() ? null : bootstrapPhoneNumber.trim(),
                null
        );

        userRepository.save(owner);

        log.info("================================================================================");
        log.info("INITIAL OWNER ACCOUNT PROVISIONED SUCCESSFULLY:");
        log.info("Username: '{}' | Role: OWNER", username);
        log.info("Password was hashed with BCrypt. Plaintext credentials were not stored or logged.");
        log.info("================================================================================");
    }
}
