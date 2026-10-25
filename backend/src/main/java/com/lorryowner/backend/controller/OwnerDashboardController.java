package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.owner.OwnerDashboardResponse;
import com.lorryowner.backend.service.OwnerDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller providing the Owner Dashboard overview API.
 * Role-gated strictly to ROLE_OWNER.
 */
@RestController
@RequestMapping("/api/owner/dashboard")
@PreAuthorize("hasRole('OWNER')")
public class OwnerDashboardController {

    private final OwnerDashboardService ownerDashboardService;

    public OwnerDashboardController(OwnerDashboardService ownerDashboardService) {
        this.ownerDashboardService = ownerDashboardService;
    }

    @GetMapping
    public ResponseEntity<OwnerDashboardResponse> getDashboard(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        OwnerDashboardResponse response = ownerDashboardService.getDashboardData(username);
        return ResponseEntity.ok(response);
    }
}
