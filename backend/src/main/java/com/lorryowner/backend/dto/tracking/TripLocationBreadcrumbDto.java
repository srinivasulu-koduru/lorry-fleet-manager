package com.lorryowner.backend.dto.tracking;

import java.time.Instant;

public record TripLocationBreadcrumbDto(
        Double latitude,
        Double longitude,
        Double speedKmh,
        Instant timestamp
) {
}
