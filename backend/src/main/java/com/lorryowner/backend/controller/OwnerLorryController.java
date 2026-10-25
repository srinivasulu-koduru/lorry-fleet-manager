package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.owner.LorryResponse;
import com.lorryowner.backend.repository.LorryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller providing Owner-only fleet lorry listing API.
 * Role-gated strictly to ROLE_OWNER.
 */
@RestController
@RequestMapping("/api/owner/lorries")
@PreAuthorize("hasRole('OWNER')")
public class OwnerLorryController {

    private final LorryRepository lorryRepository;

    public OwnerLorryController(LorryRepository lorryRepository) {
        this.lorryRepository = lorryRepository;
    }

    @GetMapping
    public ResponseEntity<List<LorryResponse>> getAllLorries() {
        List<LorryResponse> lorries = lorryRepository.findAll().stream()
                .map(l -> new LorryResponse(
                        l.getId(),
                        l.getRegistrationNumber(),
                        l.getMakeModel(),
                        l.getCapacityTons(),
                        l.getOperationalStatus(),
                        l.getIalertUnitId(),
                        l.getNotes(),
                        l.getUpdatedAt()
                ))
                .toList();
        return ResponseEntity.ok(lorries);
    }
}
