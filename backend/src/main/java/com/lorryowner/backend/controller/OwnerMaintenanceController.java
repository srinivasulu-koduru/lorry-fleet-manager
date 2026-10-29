package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.lorry.MaintenanceRecordDto;
import com.lorryowner.backend.dto.maintenance.CreateMaintenanceRecordRequest;
import com.lorryowner.backend.dto.maintenance.MaintenanceSummaryResponse;
import com.lorryowner.backend.dto.maintenance.VehicleMaintenanceDetailsResponse;
import com.lorryowner.backend.service.OwnerMaintenanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller providing Owner-level vehicle maintenance management,
 * scheduling summaries, service history, and record creation.
 * Role-gated strictly to ROLE_OWNER.
 */
@RestController
@RequestMapping("/api/owner/maintenance")
@PreAuthorize("hasRole('OWNER')")
public class OwnerMaintenanceController {

    private final OwnerMaintenanceService ownerMaintenanceService;

    public OwnerMaintenanceController(OwnerMaintenanceService ownerMaintenanceService) {
        this.ownerMaintenanceService = ownerMaintenanceService;
    }

    /**
     * Retrieves maintenance summary for the authenticated owner's fleet,
     * including vehicle status counts (Total, Due Soon, Overdue) and per-vehicle cards.
     */
    @GetMapping("/summary")
    public ResponseEntity<MaintenanceSummaryResponse> getMaintenanceSummary(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        MaintenanceSummaryResponse summary = ownerMaintenanceService.getMaintenanceSummary(username);
        return ResponseEntity.ok(summary);
    }

    /**
     * Lists all maintenance records belonging to the authenticated owner.
     */
    @GetMapping
    public ResponseEntity<List<MaintenanceRecordDto>> getAllMaintenanceRecords(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        List<MaintenanceRecordDto> records = ownerMaintenanceService.getAllMaintenanceRecords(username);
        return ResponseEntity.ok(records);
    }

    /**
     * Retrieves detailed maintenance information and service history for a specific vehicle.
     */
    @GetMapping("/vehicles/{lorryId}")
    public ResponseEntity<VehicleMaintenanceDetailsResponse> getVehicleMaintenanceDetails(
            @PathVariable Long lorryId,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        VehicleMaintenanceDetailsResponse details = ownerMaintenanceService.getLorryMaintenanceDetails(lorryId, username);
        return ResponseEntity.ok(details);
    }

    /**
     * Creates a new maintenance record under the authenticated owner's lorry.
     */
    @PostMapping
    public ResponseEntity<MaintenanceRecordDto> createMaintenanceRecord(
            @Valid @RequestBody CreateMaintenanceRecordRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        MaintenanceRecordDto saved = ownerMaintenanceService.createMaintenanceRecord(null, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
