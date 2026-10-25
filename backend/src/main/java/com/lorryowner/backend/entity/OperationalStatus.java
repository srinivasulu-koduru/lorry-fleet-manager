package com.lorryowner.backend.entity;

/**
 * Operational state of a commercial vehicle asset.
 * Strictly decoupled from trip lifecycle state.
 */
public enum OperationalStatus {
    AVAILABLE,
    ON_TRIP,
    MAINTENANCE,
    UNAVAILABLE
}
