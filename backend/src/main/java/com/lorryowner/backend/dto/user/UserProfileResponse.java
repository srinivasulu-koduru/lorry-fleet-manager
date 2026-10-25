package com.lorryowner.backend.dto.user;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String username,
        String role,
        String fullName,
        String phoneNumber,
        String licenseNumber,
        boolean isActive,
        Instant createdAt
) {
}
