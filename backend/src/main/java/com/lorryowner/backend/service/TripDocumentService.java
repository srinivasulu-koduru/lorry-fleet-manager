package com.lorryowner.backend.service;

import com.lorryowner.backend.dto.document.TripDocumentDto;
import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.Trip;
import com.lorryowner.backend.entity.TripDocument;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.TripDocumentRepository;
import com.lorryowner.backend.repository.TripRepository;
import com.lorryowner.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Service managing Trip Document lifecycle and access control.
 * Enforces multi-tenant ownership boundaries for Owners and assignment-based authorization for Drivers.
 */
@Service
public class TripDocumentService {

    private static final Logger log = LoggerFactory.getLogger(TripDocumentService.class);

    private final TripRepository tripRepository;
    private final TripDocumentRepository tripDocumentRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService storageService;

    public TripDocumentService(
            TripRepository tripRepository,
            TripDocumentRepository tripDocumentRepository,
            UserRepository userRepository,
            DocumentStorageService storageService
    ) {
        this.tripRepository = tripRepository;
        this.tripDocumentRepository = tripDocumentRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    /**
     * Uploads a new document for a trip by an authenticated Owner.
     */
    @Transactional
    public TripDocumentDto uploadDocumentForOwner(Long tripId, String documentName, MultipartFile file, String ownerUsername) {
        User owner = getAuthenticatedUser(ownerUsername, Role.OWNER);
        Trip trip = getAuthorizedTripForOwner(tripId, owner);

        String cleanDocName = (documentName != null && !documentName.trim().isBlank())
                ? documentName.trim()
                : StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "Trip Document");

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document"
        );
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        long size = file.getSize();

        String storedFilename = storageService.storeFile(file, "trips");

        TripDocument document = new TripDocument(
                trip,
                cleanDocName,
                originalFilename,
                storedFilename,
                contentType,
                size,
                owner
        );

        TripDocument saved = tripDocumentRepository.save(document);
        log.info("Owner '{}' uploaded document '{}' (ID={}) for trip ID={}",
                owner.getUsername(), saved.getDocumentName(), saved.getId(), trip.getId());

        return mapToDto(saved, "/api/owner/trips/" + tripId + "/documents/" + saved.getId() + "/download");
    }

    /**
     * Retrieves all documents for a trip for an authenticated Owner.
     */
    @Transactional(readOnly = true)
    public List<TripDocumentDto> getDocumentsForOwner(Long tripId, String ownerUsername) {
        User owner = getAuthenticatedUser(ownerUsername, Role.OWNER);
        Trip trip = getAuthorizedTripForOwner(tripId, owner);

        List<TripDocument> documents = tripDocumentRepository.findAllByTripOrderByUploadedAtDesc(trip);
        return documents.stream()
                .map(doc -> mapToDto(doc, "/api/owner/trips/" + tripId + "/documents/" + doc.getId() + "/download"))
                .toList();
    }

    /**
     * Retrieves all documents for a trip for an assigned Driver.
     */
    @Transactional(readOnly = true)
    public List<TripDocumentDto> getDocumentsForDriver(Long tripId, String driverUsername) {
        User driver = getAuthenticatedUser(driverUsername, Role.DRIVER);
        Trip trip = getAuthorizedTripForDriver(tripId, driver);

        List<TripDocument> documents = tripDocumentRepository.findAllByTripOrderByUploadedAtDesc(trip);
        return documents.stream()
                .map(doc -> mapToDto(doc, "/api/driver/trips/" + tripId + "/documents/" + doc.getId() + "/download"))
                .toList();
    }

    /**
     * Retrieves the downloadable resource and metadata for an authorized Owner or Driver.
     */
    @Transactional(readOnly = true)
    public DownloadableDocument getDocumentForDownload(Long tripId, Long documentId, String username, boolean isDriver) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found."));

        if (isDriver) {
            if (user.getRole() != Role.DRIVER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver role required.");
            }
            if (!trip.getDriver().getId().equals(user.getId())) {
                log.warn("Access violation: Driver '{}' attempted to access trip ID={} belonging to driver ID={}",
                        username, trip.getId(), trip.getDriver().getId());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not assigned to you.");
            }
        } else {
            if (user.getRole() != Role.OWNER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner role required.");
            }
            if (!trip.getOwner().getId().equals(user.getId())) {
                log.warn("Access violation: Owner '{}' attempted to access trip ID={} owned by owner ID={}",
                        username, trip.getId(), trip.getOwner().getId());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found in your fleet.");
            }
        }

        TripDocument doc = tripDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found."));

        if (!doc.getTrip().getId().equals(trip.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document does not belong to specified trip.");
        }

        Resource resource = storageService.loadFileAsResource("trips", doc.getStoredFilename());
        return new DownloadableDocument(resource, doc.getContentType(), doc.getOriginalFilename(), doc.getDocumentName());
    }

    /**
     * Deletes a trip document (Owner only).
     */
    @Transactional
    public void deleteDocumentForOwner(Long tripId, Long documentId, String ownerUsername) {
        User owner = getAuthenticatedUser(ownerUsername, Role.OWNER);
        Trip trip = getAuthorizedTripForOwner(tripId, owner);

        TripDocument doc = tripDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found."));

        if (!doc.getTrip().getId().equals(trip.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document does not belong to specified trip.");
        }

        storageService.deleteFile("trips", doc.getStoredFilename());
        tripDocumentRepository.delete(doc);

        log.info("Owner '{}' deleted document ID={} from trip ID={}",
                owner.getUsername(), documentId, trip.getId());
    }

    private User getAuthenticatedUser(String username, Role expectedRole) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        if (user.getRole() != expectedRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: " + expectedRole + " role required.");
        }
        return user;
    }

    private Trip getAuthorizedTripForOwner(Long tripId, User owner) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + tripId));
        if (!trip.getOwner().getId().equals(owner.getId())) {
            log.warn("Access violation: Owner '{}' attempted to access trip ID={} owned by owner ID={}",
                    owner.getUsername(), trip.getId(), trip.getOwner().getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found in your fleet.");
        }
        return trip;
    }

    private Trip getAuthorizedTripForDriver(Long tripId, User driver) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + tripId));
        if (!trip.getDriver().getId().equals(driver.getId())) {
            log.warn("Access violation: Driver '{}' attempted to access trip ID={} assigned to driver ID={}",
                    driver.getUsername(), trip.getId(), trip.getDriver().getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not assigned to you.");
        }
        return trip;
    }

    private TripDocumentDto mapToDto(TripDocument doc, String downloadUrl) {
        return new TripDocumentDto(
                doc.getId(),
                doc.getTrip().getId(),
                doc.getDocumentName(),
                doc.getOriginalFilename(),
                doc.getContentType(),
                doc.getFileSize(),
                doc.getUploadedAt(),
                doc.getUploadedBy() != null ? doc.getUploadedBy().getFullName() : null,
                downloadUrl
        );
    }

    public record DownloadableDocument(Resource resource, String contentType, String originalFilename, String documentName) {}
}
