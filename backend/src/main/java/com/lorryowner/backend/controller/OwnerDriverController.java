package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.driver.CreateDriverRequest;
import com.lorryowner.backend.dto.driver.DriverResponse;
import com.lorryowner.backend.service.OwnerDriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller providing Owner-only driver account management.
 * Enforced via @PreAuthorize("hasRole('OWNER')") and SecurityConfig URL rules.
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
     * Creates a new permanent driver account.
     */
    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody CreateDriverRequest request) {
        DriverResponse response = ownerDriverService.createDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all permanent driver accounts in the fleet.
     */
    @GetMapping
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {
        return ResponseEntity.ok(ownerDriverService.getAllDrivers());
    }
}
