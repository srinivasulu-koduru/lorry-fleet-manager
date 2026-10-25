package com.lorryowner.backend.dto.owner;

import com.lorryowner.backend.entity.OperationalStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record LorryResponse(
    Long id,
    String registrationNumber,
    String makeModel,
    BigDecimal capacityTons,
    OperationalStatus operationalStatus,
    String ialertUnitId,
    String notes,
    Instant updatedAt
) {}
