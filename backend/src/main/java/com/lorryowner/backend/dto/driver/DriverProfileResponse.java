package com.lorryowner.backend.dto.driver;

import java.time.Instant;

/**
 * Driver profile response containing authenticated driver details,
 * contact info, license number, and assigned lorry.
 */
public record DriverProfileResponse(
        Long id,
        String username,
        String role,
        String fullName,
        String phoneNumber,
        String licenseNumber,
        String assignedLorryRegistration,
        String assignedLorryMakeModel,
        boolean isActive,
        Instant createdAt
) {}
