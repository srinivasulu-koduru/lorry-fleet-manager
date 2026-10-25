package com.lorryowner.backend.dto.owner;

public record FleetOverviewDto(
    long runningNow,
    long readyToStart,
    long notReady,
    long totalLorries
) {}
