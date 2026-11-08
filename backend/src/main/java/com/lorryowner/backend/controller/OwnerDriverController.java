package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.driver.*;
import com.lorryowner.backend.entity.TripStatus;
import com.lorryowner.backend.service.OwnerDriverService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for Fleet Owner Driver Management, Trip History, and Performance Analytics.
 * All endpoints require authenticated access with ROLE_OWNER authority.
 */
@RestController
@RequestMapping("/api/owner/drivers")
@PreAuthorize("hasRole('OWNER')")
public class OwnerDriverController {

    private final OwnerDriverService ownerDriverService;

    public OwnerDriverController(OwnerDriverService ownerDriverService) {
        this.ownerDriverService = ownerDriverService;
    }

    /**
     * Registers a new permanent driver account.
     */
    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(
            @Valid @RequestBody CreateDriverRequest request,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        DriverResponse response = ownerDriverService.createDriver(request, ownerUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all permanent driver accounts for the authenticated owner with optional search & filter.
     */
    @GetMapping
    public ResponseEntity<List<DriverResponse>> getAllDrivers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assigned,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ownerDriverService.getAllDrivers(ownerUsername, search, status, assigned));
    }

    /**
     * Lists only available drivers eligible for immediate trip dispatch (excluding drivers on active trips).
     * GET /api/owner/drivers/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers(Authentication authentication) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ownerDriverService.getAvailableDrivers(ownerUsername));
    }

    /**
     * Retrieves details for a specific driver.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriverById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ownerDriverService.getDriverById(id, ownerUsername));
    }

    /**
     * Updates permitted driver details (name, phone, license, lorry assignment).
     */
    @PutMapping("/{id}")
    public ResponseEntity<DriverResponse> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDriverRequest request,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ownerDriverService.updateDriver(id, request, ownerUsername));
    }

    /**
     * Explicitly assigns or unassigns a lorry to a driver.
     * Supports both /{id}/assign-lorry and /{id}/lorry.
     */
    @PutMapping(path = {"/{id}/assign-lorry", "/{id}/lorry"})
    public ResponseEntity<DriverResponse> assignLorry(
            @PathVariable Long id,
            @RequestBody(required = false) AssignLorryRequest request,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ownerDriverService.assignLorry(id, request, ownerUsername));
    }

    /**
     * Toggles driver active/inactive status (PATCH).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<DriverResponse> updateDriverStatusPatch(
            @PathVariable Long id,
            @Valid @RequestBody DriverStatusRequest request,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ownerDriverService.updateDriverStatus(id, request.isActive(), ownerUsername));
    }

    /**
     * Toggles driver active/inactive status (PUT).
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<DriverResponse> updateDriverStatusPut(
            @PathVariable Long id,
            @Valid @RequestBody DriverStatusRequest request,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ownerDriverService.updateDriverStatus(id, request.isActive(), ownerUsername));
    }

    /**
     * Safely deactivates or removes a driver account.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<DriverResponse> deleteDriver(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ownerDriverService.deleteDriver(id, ownerUsername));
    }

    // --- Driver Trip History and Performance Analytics ---

    /**
     * Lists paginated and filtered trips for a specific driver.
     */
    @GetMapping("/{id}/trips")
    public ResponseEntity<TripListResponse> getDriverTrips(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String lorryRegistration,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        TripStatus tripStatus = null;
        if (status != null && !status.trim().isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                tripStatus = TripStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(ownerDriverService.getDriverTrips(
                id, ownerUsername, tripStatus, startDate, endDate, lorryRegistration, origin, destination, page, size
        ));
    }

    /**
     * Retrieves full details for a specific trip of a driver.
     */
    @GetMapping("/{id}/trips/{tripId}")
    public ResponseEntity<TripDto> getDriverTripById(
            @PathVariable Long id,
            @PathVariable Long tripId,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ownerDriverService.getDriverTripById(id, tripId, ownerUsername));
    }

    /**
     * Retrieves driver performance metrics aggregated from real trips.
     */
    @GetMapping("/{id}/performance")
    public ResponseEntity<DriverPerformanceDto> getDriverPerformance(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ownerDriverService.getDriverPerformance(id, ownerUsername));
    }
}
