package com.lorryowner.backend.dto.driver;

public record AssignLorryRequest(
        Long lorryId,
        String registrationNumber
) {
}
