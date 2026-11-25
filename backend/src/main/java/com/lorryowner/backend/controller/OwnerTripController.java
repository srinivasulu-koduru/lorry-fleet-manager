package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.driver.TripListResponse;
import com.lorryowner.backend.dto.driver.UpdateTripNotesRequest;
import com.lorryowner.backend.dto.lorry.MaintenanceRecordDto;
import com.lorryowner.backend.dto.maintenance.CreateMaintenanceRecordRequest;
import com.lorryowner.backend.dto.trip.CreateTripExpenseRequest;
import com.lorryowner.backend.dto.trip.TripCreateDto;
import com.lorryowner.backend.dto.trip.TripDetailResponseDto;
import com.lorryowner.backend.dto.trip.TripResponseDto;
import com.lorryowner.backend.service.OwnerTripService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
     * Retrieves paginated owner trip history with optional status and search filtering.
     * GET /api/owner/trips
     */
    @GetMapping
    public ResponseEntity<TripListResponse> getOwnerTrips(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        TripListResponse response = ownerTripService.getOwnerTrips(
                status, search, startDate, endDate, page, size, ownerUsername
        );
        return ResponseEntity.ok(response);
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
     * Retrieves basic trip response by trip ID.
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
     * Retrieves complete trip details including financial breakdown (revenue, itemized maintenance,
     * fuel, driver, other expenses, total expenses, net profit).
     * GET /api/owner/trips/{id}/details
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<TripDetailResponseDto> getTripDetails(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        TripDetailResponseDto response = ownerTripService.getTripDetails(id, ownerUsername);
        return ResponseEntity.ok(response);
    }

    /**
     * Adds a maintenance record specifically linked to a trip.
     * Automatically derives the lorry from the trip.
     * POST /api/owner/trips/{id}/maintenance
     */
    @PostMapping("/{id}/maintenance")
    public ResponseEntity<MaintenanceRecordDto> addTripMaintenance(
            @PathVariable Long id,
            @Valid @RequestBody CreateMaintenanceRecordRequest request,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        MaintenanceRecordDto created = ownerTripService.addTripMaintenance(id, request, ownerUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Adds an expense or maintenance entry inside a trip.
     * Automatically derives the lorry from the trip.
     * POST /api/owner/trips/{id}/expenses
     */
    @PostMapping("/{id}/expenses")
    public ResponseEntity<?> addTripExpense(
            @PathVariable Long id,
            @Valid @RequestBody CreateTripExpenseRequest request,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        Object created = ownerTripService.addTripExpense(id, request, ownerUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Deletes a maintenance record associated with a trip.
     * DELETE /api/owner/trips/{tripId}/maintenance/{maintenanceId}
     */
    @DeleteMapping("/{tripId}/maintenance/{maintenanceId}")
    public ResponseEntity<Void> deleteTripMaintenance(
            @PathVariable Long tripId,
            @PathVariable Long maintenanceId,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        ownerTripService.deleteTripMaintenance(tripId, maintenanceId, ownerUsername);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes an expense transaction associated with a trip.
     * DELETE /api/owner/trips/{tripId}/expenses/{expenseId}
     */
    @DeleteMapping("/{tripId}/expenses/{expenseId}")
    public ResponseEntity<Void> deleteTripExpense(
            @PathVariable Long tripId,
            @PathVariable Long expenseId,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        ownerTripService.deleteTripExpense(tripId, expenseId, ownerUsername);
        return ResponseEntity.noContent().build();
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
