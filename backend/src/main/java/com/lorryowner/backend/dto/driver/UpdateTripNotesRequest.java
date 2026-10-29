package com.lorryowner.backend.dto.driver;

/**
 * Optional request payload when completing or updating a trip.
 */
public record UpdateTripNotesRequest(
        String notes
) {}
