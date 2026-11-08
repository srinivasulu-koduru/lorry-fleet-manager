package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.driver.UpdateTripNotesRequest;
import com.lorryowner.backend.dto.trip.TripCreateDto;
import com.lorryowner.backend.dto.trip.TripResponseDto;
import com.lorryowner.backend.service.OwnerTripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Owner Trip Management.
 * Enforces authenticated role-gated access (ROLE_OWNER).
 */
@RestController
@RequestMapping("/api/owner/trips")
@PreAuthorize("hasRole('OWNER')")
public class OwnerTripController {

    private final OwnerTripService ownerTripService;

    public OwnerTripController(OwnerTripService ownerTripService) {
        this.ownerTripService = ownerTripService;
    }

    /**
     * Creates a new planned trip in the fleet.
     * POST /api/owner/trips
     */
    @PostMapping
    public ResponseEntity<TripResponseDto> createTrip(
            @Valid @RequestBody TripCreateDto request,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        TripResponseDto created = ownerTripService.createTrip(request, ownerUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Retrieves trip details by trip ID.
     * GET /api/owner/trips/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TripResponseDto> getTripById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        TripResponseDto response = ownerTripService.getTripById(id, ownerUsername);
        return ResponseEntity.ok(response);
    }

    /**
     * Starts a planned trip.
     * POST /api/owner/trips/{id}/start
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<TripResponseDto> startTrip(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        TripResponseDto response = ownerTripService.startTrip(id, ownerUsername);
        return ResponseEntity.ok(response);
    }

    /**
     * Completes an in-progress trip.
     * POST /api/owner/trips/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<TripResponseDto> completeTrip(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateTripNotesRequest request,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        String notes = request != null ? request.notes() : null;
        TripResponseDto response = ownerTripService.completeTrip(id, notes, ownerUsername);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels a planned trip.
     * POST /api/owner/trips/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<TripResponseDto> cancelTrip(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        TripResponseDto cancelled = ownerTripService.cancelTrip(id, ownerUsername);
        return ResponseEntity.ok(cancelled);
    }
}
