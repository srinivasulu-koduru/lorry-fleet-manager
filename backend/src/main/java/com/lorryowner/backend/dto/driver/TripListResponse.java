package com.lorryowner.backend.dto.driver;

import java.util.List;

public record TripListResponse(
        List<TripDto> trips,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {}
