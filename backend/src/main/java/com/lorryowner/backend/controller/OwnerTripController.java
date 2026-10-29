package com.lorryowner.backend.controller;

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
}
