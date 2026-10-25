package com.lorryowner.backend.dto.owner;

import java.time.Instant;

public record RecentActivityDto(
    Long id,
    String registrationNumber,
    String statusDescription,
    String activityType,
    String colorIndicator,
    String timeAgo,
    Instant timestamp
) {}
