package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.document.TripDocumentDto;
import com.lorryowner.backend.dto.document.VehicleDocumentDto;
import com.lorryowner.backend.service.TripDocumentService;
import com.lorryowner.backend.service.VehicleDocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller providing Trip Document and Vehicle Document management for authenticated Owners.
 * Role-gated strictly to ROLE_OWNER.
 */
@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('OWNER')")
public class OwnerDocumentController {

    private final TripDocumentService tripDocumentService;
    private final VehicleDocumentService vehicleDocumentService;

    public OwnerDocumentController(
            TripDocumentService tripDocumentService,
            VehicleDocumentService vehicleDocumentService
    ) {
        this.tripDocumentService = tripDocumentService;
        this.vehicleDocumentService = vehicleDocumentService;
    }

    // =========================================================================
    // TRIP DOCUMENTS (OWNER)
    // =========================================================================

    @PostMapping(value = "/trips/{tripId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TripDocumentDto> uploadTripDocument(
            @PathVariable Long tripId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentName", required = false) String documentName,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        TripDocumentDto saved = tripDocumentService.uploadDocumentForOwner(tripId, documentName, file, ownerUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/trips/{tripId}/documents")
    public ResponseEntity<List<TripDocumentDto>> getTripDocuments(
            @PathVariable Long tripId,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        List<TripDocumentDto> docs = tripDocumentService.getDocumentsForOwner(tripId, ownerUsername);
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/trips/{tripId}/documents/{docId}/download")
    public ResponseEntity<Resource> downloadTripDocument(
            @PathVariable Long tripId,
            @PathVariable Long docId,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        TripDocumentService.DownloadableDocument doc = tripDocumentService.getDocumentForDownload(
                tripId, docId, ownerUsername, false
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.originalFilename() + "\"")
                .body(doc.resource());
    }

    @DeleteMapping("/trips/{tripId}/documents/{docId}")
    public ResponseEntity<Void> deleteTripDocument(
            @PathVariable Long tripId,
            @PathVariable Long docId,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        tripDocumentService.deleteDocumentForOwner(tripId, docId, ownerUsername);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // VEHICLE DOCUMENTS (OWNER)
    // =========================================================================

    @PostMapping(value = "/lorries/{lorryId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VehicleDocumentDto> uploadVehicleDocument(
            @PathVariable Long lorryId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentName", required = false) String documentName,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        VehicleDocumentDto saved = vehicleDocumentService.uploadDocumentForOwner(lorryId, documentName, file, ownerUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/lorries/{lorryId}/documents")
    public ResponseEntity<List<VehicleDocumentDto>> getVehicleDocuments(
            @PathVariable Long lorryId,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        List<VehicleDocumentDto> docs = vehicleDocumentService.getDocumentsForOwner(lorryId, ownerUsername);
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/lorries/{lorryId}/documents/{docId}/download")
    public ResponseEntity<Resource> downloadVehicleDocument(
            @PathVariable Long lorryId,
            @PathVariable Long docId,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        TripDocumentService.DownloadableDocument doc = vehicleDocumentService.getDocumentForDownload(
                lorryId, docId, ownerUsername, false
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.originalFilename() + "\"")
                .body(doc.resource());
    }

    @DeleteMapping("/lorries/{lorryId}/documents/{docId}")
    public ResponseEntity<Void> deleteVehicleDocument(
            @PathVariable Long lorryId,
            @PathVariable Long docId,
            Authentication authentication
    ) {
        String ownerUsername = authentication != null ? authentication.getName() : null;
        vehicleDocumentService.deleteDocumentForOwner(lorryId, docId, ownerUsername);
        return ResponseEntity.noContent().build();
    }
}
