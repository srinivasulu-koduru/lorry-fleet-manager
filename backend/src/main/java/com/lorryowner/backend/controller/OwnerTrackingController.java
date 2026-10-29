package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.tracking.LiveTrackingDetailsResponse;
import com.lorryowner.backend.dto.tracking.LiveTrackingLorryDto;
import com.lorryowner.backend.service.LiveTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for authenticated Fleet Owners to view real-time fleet GPS tracking.
 */
@RestController
@RequestMapping("/api/owner/tracking")
@PreAuthorize("hasRole('OWNER')")
public class OwnerTrackingController {

    private final LiveTrackingService liveTrackingService;

    public OwnerTrackingController(LiveTrackingService liveTrackingService) {
        this.liveTrackingService = liveTrackingService;
    }

    /**
     * Lists all running lorries (with active IN_PROGRESS trips) and their live tracking state.
     * GET /api/owner/tracking/running-lorries
     */
    @GetMapping("/running-lorries")
    public ResponseEntity<List<LiveTrackingLorryDto>> getRunningLorries(Authentication authentication) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        List<LiveTrackingLorryDto> runningLorries = liveTrackingService.getRunningLorriesForOwner(ownerUsername);
        return ResponseEntity.ok(runningLorries);
    }

    /**
     * Fetches detailed live tracking, breadcrumbs, and route points for a specific trip.
     * GET /api/owner/tracking/trips/{tripId}
     */
    @GetMapping("/trips/{tripId}")
    public ResponseEntity<LiveTrackingDetailsResponse> getTripTrackingDetails(
            @PathVariable Long tripId,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        LiveTrackingDetailsResponse response = liveTrackingService.getTripLiveTrackingDetails(tripId, ownerUsername);
        return ResponseEntity.ok(response);
    }

    /**
     * Fetches detailed live tracking for a specific running lorry.
     * GET /api/owner/tracking/lorries/{lorryId}
     */
    @GetMapping("/lorries/{lorryId}")
    public ResponseEntity<LiveTrackingDetailsResponse> getLorryTrackingDetails(
            @PathVariable Long lorryId,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        LiveTrackingDetailsResponse response = liveTrackingService.getLorryLiveTrackingDetails(lorryId, ownerUsername);
        return ResponseEntity.ok(response);
    }
}
