package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.driver.TripListResponse;
import com.lorryowner.backend.dto.lorry.CreateLorryRequest;
import com.lorryowner.backend.dto.lorry.FleetSummaryDto;
import com.lorryowner.backend.dto.lorry.LorryMaintenanceResponse;
import com.lorryowner.backend.dto.lorry.MaintenanceRecordDto;
import com.lorryowner.backend.dto.lorry.OwnerLorryDetailDto;
import com.lorryowner.backend.dto.maintenance.CreateMaintenanceRecordRequest;
import com.lorryowner.backend.service.OwnerLorryService;
import com.lorryowner.backend.service.OwnerMaintenanceService;
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
 * Controller providing Owner-only fleet lorry listing, details, trip history,
 * maintenance records, and fleet status summary APIs.
 * Role-gated strictly to ROLE_OWNER.
 */
@RestController
@RequestMapping("/api/owner/lorries")
@PreAuthorize("hasRole('OWNER')")
public class OwnerLorryController {

    private final OwnerLorryService ownerLorryService;
    private final OwnerMaintenanceService ownerMaintenanceService;

    public OwnerLorryController(
            OwnerLorryService ownerLorryService,
            OwnerMaintenanceService ownerMaintenanceService
    ) {
        this.ownerLorryService = ownerLorryService;
        this.ownerMaintenanceService = ownerMaintenanceService;
    }

    /**
     * Registers a new commercial fleet lorry under the authenticated owner.
     * Optionally assigns an eligible driver.
     */
    @PostMapping
    public ResponseEntity<OwnerLorryDetailDto> createLorry(
            @Valid @RequestBody CreateLorryRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        OwnerLorryDetailDto saved = ownerLorryService.createLorry(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Retrieves fleet lorries with optional status filtering (ALL, RUNNING, NOT_RUNNING)
     * and search query (matching registration number, model, or assigned driver).
     */
    @GetMapping
    public ResponseEntity<List<OwnerLorryDetailDto>> getAllLorries(
            @RequestParam(required = false, defaultValue = "ALL") String filter,
            @RequestParam(required = false) String search,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        List<OwnerLorryDetailDto> lorries = ownerLorryService.getOwnerLorries(username, filter, search);
        return ResponseEntity.ok(lorries);
    }

    /**
     * Retrieves high-level fleet status summary (total, running, not-running,
     * percentages, total distance, total maintenance cost).
     */
    @GetMapping("/summary")
    public ResponseEntity<FleetSummaryDto> getFleetSummary(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        FleetSummaryDto summary = ownerLorryService.getFleetSummary(username);
        return ResponseEntity.ok(summary);
    }

    /**
     * Retrieves complete details for a single lorry including assigned driver,
     * current active trip, trip statistics, and maintenance totals.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OwnerLorryDetailDto> getLorryDetails(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        OwnerLorryDetailDto details = ownerLorryService.getLorryDetails(id, username);
        return ResponseEntity.ok(details);
    }

    /**
     * Retrieves historical trips associated with a specific lorry,
     * ordered chronologically (newest first) with status and search filtering.
     */
    @GetMapping("/{id}/trips")
    public ResponseEntity<TripListResponse> getLorryTrips(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        TripListResponse response = ownerLorryService.getLorryTrips(
                id, status, search, startDate, endDate, page, size, username
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves service and maintenance records for a specific lorry,
     * including cumulative maintenance expenditure.
     */
    @GetMapping("/{id}/maintenance")
    public ResponseEntity<LorryMaintenanceResponse> getLorryMaintenance(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        LorryMaintenanceResponse response = ownerLorryService.getLorryMaintenance(id, username);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new maintenance record for a specific lorry owned by the authenticated owner.
     */
    @PostMapping("/{id}/maintenance")
    public ResponseEntity<MaintenanceRecordDto> createLorryMaintenance(
            @PathVariable Long id,
            @Valid @RequestBody CreateMaintenanceRecordRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        MaintenanceRecordDto saved = ownerMaintenanceService.createMaintenanceRecord(id, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
