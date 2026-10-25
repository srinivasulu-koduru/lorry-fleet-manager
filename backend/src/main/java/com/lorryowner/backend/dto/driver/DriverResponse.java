package com.lorryowner.backend.dto.driver;

import java.time.Instant;

public record DriverResponse(
        Long id,
        String username,
        String fullName,
        String phoneNumber,
        String licenseNumber,
        boolean isActive,
        Instant createdAt
) {
}
