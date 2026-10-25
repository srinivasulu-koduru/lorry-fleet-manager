package com.lorryowner.backend.service;

import com.lorryowner.backend.dto.driver.CreateDriverRequest;
import com.lorryowner.backend.dto.driver.DriverResponse;
import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Service managing permanent driver accounts for the Fleet Owner.
 */
@Service
public class OwnerDriverService {

    private static final Logger log = LoggerFactory.getLogger(OwnerDriverService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public OwnerDriverService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public DriverResponse createDriver(CreateDriverRequest request) {
        String username = request.username().trim();

        if (userRepository.existsByUsername(username)) {
            log.warn("Driver creation failed: Username '{}' already taken", username);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username '" + username + "' is already in use.");
        }

        User driver = new User(
                username,
                passwordEncoder.encode(request.password()),
                Role.DRIVER,
                request.fullName().trim(),
                request.phoneNumber() != null ? request.phoneNumber().trim() : null,
                request.licenseNumber() != null ? request.licenseNumber().trim() : null
        );

        User saved = userRepository.save(driver);
        log.info("Permanent Driver account created successfully: ID={}, username='{}'", saved.getId(), saved.getUsername());

        return mapToDriverResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> getAllDrivers() {
        return userRepository.findByRole(Role.DRIVER).stream()
                .map(this::mapToDriverResponse)
                .toList();
    }

    private DriverResponse mapToDriverResponse(User user) {
        return new DriverResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getLicenseNumber(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
