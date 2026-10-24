package com.lorryowner.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorryowner.backend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Instant;

/**
 * Spring Security configuration for the Lorry Owner backend.
 *
 * Design decisions:
 * 1. Stateless session policy: Tailored for mobile REST clients (Android Jetpack Compose).
 * 2. Strict least-privilege access: Only explicitly declared endpoints (like /api/health) are public.
 *    All other endpoints require authentication.
 * 3. Consistent JSON 401 response: Unauthenticated API calls receive clean JSON rather than HTML redirects.
 * 4. Extensible role support: Prepared for future OWNER and DRIVER roles (e.g., ROLE_OWNER, ROLE_DRIVER).
 * 5. Production-grade password encoding: BCrypt is provided as the standard PasswordEncoder bean.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless REST API accessed by Android mobile clients
                .csrf(AbstractHttpConfigurer::disable)

                // Enforce stateless session management (no HTTP session cookies)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Configure endpoint authorization
                .authorizeHttpRequests(auth -> auth
                        // Public health and actuator endpoints
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // In future phases:
                        // .requestMatchers("/api/owner/**").hasRole("OWNER")
                        // .requestMatchers("/api/driver/**").hasRole("DRIVER")
                        // All other endpoints require authentication by default
                        .anyRequest().authenticated()
                )

                // Return structured JSON 401 Unauthorized for unauthenticated requests
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                )

                // Enable standard HTTP Basic for initial testing / development support
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /**
     * BCrypt password encoder bean for hashing passwords securely.
     * Uses BCrypt standard work factor (default 10 rounds).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Entry point to return clean JSON error instead of Spring's default HTML login page.
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ErrorResponse error = new ErrorResponse(
                    Instant.now(),
                    HttpStatus.UNAUTHORIZED.value(),
                    HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                    "Full authentication is required to access this resource.",
                    request.getRequestURI()
            );

            response.getWriter().write(objectMapper.writeValueAsString(error));
        };
    }
}
