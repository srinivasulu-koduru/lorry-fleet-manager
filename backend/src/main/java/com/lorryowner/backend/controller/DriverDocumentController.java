package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.document.TripDocumentDto;
import com.lorryowner.backend.dto.document.VehicleDocumentDto;
import com.lorryowner.backend.service.TripDocumentService;
import com.lorryowner.backend.service.VehicleDocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller providing read and download access to Trip Documents and Vehicle Documents
 * for authenticated Drivers assigned to the respective trip or lorry.
 * Role-gated strictly to ROLE_DRIVER. Drivers cannot upload or delete documents.
 */
@RestController
@RequestMapping("/api/driver")
@PreAuthorize("hasRole('DRIVER')")
public class DriverDocumentController {

    private final TripDocumentService tripDocumentService;
    private final VehicleDocumentService vehicleDocumentService;

    public DriverDocumentController(
            TripDocumentService tripDocumentService,
            VehicleDocumentService vehicleDocumentService
    ) {
        this.tripDocumentService = tripDocumentService;
        this.vehicleDocumentService = vehicleDocumentService;
    }

    // =========================================================================
    // TRIP DOCUMENTS (DRIVER)
    // =========================================================================

    @GetMapping("/trips/{tripId}/documents")
    public ResponseEntity<List<TripDocumentDto>> getTripDocuments(
            @PathVariable Long tripId,
            Authentication authentication
    ) {
        String driverUsername = authentication != null ? authentication.getName() : null;
        List<TripDocumentDto> docs = tripDocumentService.getDocumentsForDriver(tripId, driverUsername);
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/trips/{tripId}/documents/{docId}/download")
    public ResponseEntity<Resource> downloadTripDocument(
            @PathVariable Long tripId,
            @PathVariable Long docId,
            Authentication authentication
    ) {
        String driverUsername = authentication != null ? authentication.getName() : null;
        TripDocumentService.DownloadableDocument doc = tripDocumentService.getDocumentForDownload(
                tripId, docId, driverUsername, true
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.originalFilename() + "\"")
                .body(doc.resource());
    }

    // =========================================================================
    // VEHICLE DOCUMENTS (DRIVER)
    // =========================================================================

    @GetMapping("/lorries/{lorryId}/documents")
    public ResponseEntity<List<VehicleDocumentDto>> getVehicleDocuments(
            @PathVariable Long lorryId,
            Authentication authentication
    ) {
        String driverUsername = authentication != null ? authentication.getName() : null;
        List<VehicleDocumentDto> docs = vehicleDocumentService.getDocumentsForDriver(lorryId, driverUsername);
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/lorries/{lorryId}/documents/{docId}/download")
    public ResponseEntity<Resource> downloadVehicleDocument(
            @PathVariable Long lorryId,
            @PathVariable Long docId,
            Authentication authentication
    ) {
        String driverUsername = authentication != null ? authentication.getName() : null;
        TripDocumentService.DownloadableDocument doc = vehicleDocumentService.getDocumentForDownload(
                lorryId, docId, driverUsername, true
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.originalFilename() + "\"")
                .body(doc.resource());
    }
}
