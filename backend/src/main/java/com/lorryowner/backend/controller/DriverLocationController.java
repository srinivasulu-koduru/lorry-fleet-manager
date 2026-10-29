package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.tracking.LiveTrackingLorryDto;
import com.lorryowner.backend.dto.tracking.SubmitLocationRequest;
import com.lorryowner.backend.service.LiveTrackingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authenticated Drivers to post real-time GPS coordinates during an active trip.
 */
@RestController
@RequestMapping("/api/driver/trips")
@PreAuthorize("hasRole('DRIVER')")
public class DriverLocationController {

    private final LiveTrackingService liveTrackingService;

    public DriverLocationController(LiveTrackingService liveTrackingService) {
        this.liveTrackingService = liveTrackingService;
    }

    /**
     * Submits live GPS coordinate update from the authenticated driver's phone.
     * POST /api/driver/trips/{tripId}/location
     */
    @PostMapping("/{tripId}/location")
    public ResponseEntity<LiveTrackingLorryDto> submitLocation(
            @PathVariable Long tripId,
            @Valid @RequestBody SubmitLocationRequest request,
            Authentication authentication
    ) {
        String driverUsername = authentication != null ? authentication.getName() : null;
        LiveTrackingLorryDto updated = liveTrackingService.submitDriverLocation(tripId, request, driverUsername);
        return ResponseEntity.ok(updated);
    }
}
