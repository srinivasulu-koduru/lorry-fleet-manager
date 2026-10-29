package com.lorryowner.backend.dto.driver;

import java.math.BigDecimal;
import java.time.Instant;

public record DriverResponse(
        Long id,
        String username,
        String fullName,
        String phoneNumber,
        String licenseNumber,
        boolean isActive,
        Instant createdAt,
        String assignedLorryRegistration,
        Long assignedLorryId,
        String ownerUsername,
        long completedTrips,
        BigDecimal totalRecordedDistanceKm
) {
    public DriverResponse(Long id, String username, String fullName, String phoneNumber, String licenseNumber, boolean isActive, Instant createdAt) {
        this(id, username, fullName, phoneNumber, licenseNumber, isActive, createdAt, null, null, null, 0L, BigDecimal.ZERO);
    }

    public DriverResponse(Long id, String username, String fullName, String phoneNumber, String licenseNumber, boolean isActive, Instant createdAt, String assignedLorryRegistration, Long assignedLorryId, String ownerUsername) {
        this(id, username, fullName, phoneNumber, licenseNumber, isActive, createdAt, assignedLorryRegistration, assignedLorryId, ownerUsername, 0L, BigDecimal.ZERO);
    }
}
