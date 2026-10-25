package com.lorryowner.backend.service;

import com.lorryowner.backend.dto.auth.LoginRequest;
import com.lorryowner.backend.dto.auth.LoginResponse;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.UserRepository;
import com.lorryowner.backend.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user authentication and JWT issuance.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Login failed: User '{}' not found", username);
                    return new BadCredentialsException("Invalid username or password.");
                });

        if (!user.isActive()) {
            log.warn("Login failed: User '{}' account is inactive/disabled", username);
            throw new BadCredentialsException("Invalid username or password.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed: Incorrect password for user '{}'", username);
            throw new BadCredentialsException("Invalid username or password.");
        }

        String accessToken = jwtService.generateToken(user);
        long expiresIn = jwtService.getExpirationMs();

        log.info("User '{}' successfully authenticated with trusted role '{}'", username, user.getRole().name());

        return new LoginResponse(
                accessToken,
                "Bearer",
                expiresIn,
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getFullName()
        );
    }
}
