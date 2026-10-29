package com.lorryowner.backend.dto.driver;

import jakarta.validation.constraints.NotNull;

public record DriverStatusRequest(
        @NotNull(message = "isActive flag is required")
        Boolean isActive
) {
}
