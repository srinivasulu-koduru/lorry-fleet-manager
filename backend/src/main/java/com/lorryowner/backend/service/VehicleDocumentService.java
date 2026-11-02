package com.lorryowner.backend.service;

import com.lorryowner.backend.dto.document.VehicleDocumentDto;
import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.entity.VehicleDocument;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.TripRepository;
import com.lorryowner.backend.repository.UserRepository;
import com.lorryowner.backend.repository.VehicleDocumentRepository;
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
 * Service managing Vehicle Document lifecycle and access control.
 * Enforces multi-tenant ownership boundaries for Owners and assignment-based authorization for Drivers.
 */
@Service
public class VehicleDocumentService {

    private static final Logger log = LoggerFactory.getLogger(VehicleDocumentService.class);

    private final LorryRepository lorryRepository;
    private final VehicleDocumentRepository vehicleDocumentRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final DocumentStorageService storageService;

    public VehicleDocumentService(
            LorryRepository lorryRepository,
            VehicleDocumentRepository vehicleDocumentRepository,
            UserRepository userRepository,
            TripRepository tripRepository,
            DocumentStorageService storageService
    ) {
        this.lorryRepository = lorryRepository;
        this.vehicleDocumentRepository = vehicleDocumentRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.storageService = storageService;
    }

    /**
     * Uploads a new vehicle document for a lorry by an authenticated Owner.
     */
    @Transactional
    public VehicleDocumentDto uploadDocumentForOwner(Long lorryId, String documentName, MultipartFile file, String ownerUsername) {
        User owner = getAuthenticatedUser(ownerUsername, Role.OWNER);
        Lorry lorry = getAuthorizedLorryForOwner(lorryId, owner);

        String cleanDocName = (documentName != null && !documentName.trim().isBlank())
                ? documentName.trim()
                : StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "Vehicle Document");

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document"
        );
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        long size = file.getSize();

        String storedFilename = storageService.storeFile(file, "vehicles");

        VehicleDocument document = new VehicleDocument(
                lorry,
                cleanDocName,
                originalFilename,
                storedFilename,
                contentType,
                size,
                owner
        );

        VehicleDocument saved = vehicleDocumentRepository.save(document);
        log.info("Owner '{}' uploaded vehicle document '{}' (ID={}) for lorry ID={}",
                owner.getUsername(), saved.getDocumentName(), saved.getId(), lorry.getId());

        return mapToDto(saved, "/api/owner/lorries/" + lorryId + "/documents/" + saved.getId() + "/download");
    }

    /**
     * Retrieves all documents for a lorry for an authenticated Owner.
     */
    @Transactional(readOnly = true)
    public List<VehicleDocumentDto> getDocumentsForOwner(Long lorryId, String ownerUsername) {
        User owner = getAuthenticatedUser(ownerUsername, Role.OWNER);
        Lorry lorry = getAuthorizedLorryForOwner(lorryId, owner);

        List<VehicleDocument> documents = vehicleDocumentRepository.findAllByLorryOrderByUploadedAtDesc(lorry);
        return documents.stream()
                .map(doc -> mapToDto(doc, "/api/owner/lorries/" + lorryId + "/documents/" + doc.getId() + "/download"))
                .toList();
    }

    /**
     * Retrieves all documents for a lorry for an authorized Driver
     * (driver must be assigned to the lorry or assigned to a trip on that lorry).
     */
    @Transactional(readOnly = true)
    public List<VehicleDocumentDto> getDocumentsForDriver(Long lorryId, String driverUsername) {
        User driver = getAuthenticatedUser(driverUsername, Role.DRIVER);
        Lorry lorry = getAuthorizedLorryForDriver(lorryId, driver);

        List<VehicleDocument> documents = vehicleDocumentRepository.findAllByLorryOrderByUploadedAtDesc(lorry);
        return documents.stream()
                .map(doc -> mapToDto(doc, "/api/driver/lorries/" + lorryId + "/documents/" + doc.getId() + "/download"))
                .toList();
    }

    /**
     * Retrieves the downloadable resource and metadata for an authorized Owner or Driver.
     */
    @Transactional(readOnly = true)
    public TripDocumentService.DownloadableDocument getDocumentForDownload(Long lorryId, Long documentId, String username, boolean isDriver) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        Lorry lorry = lorryRepository.findById(lorryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lorry not found."));

        if (isDriver) {
            if (user.getRole() != Role.DRIVER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver role required.");
            }
            boolean isAssignedLorry = user.getAssignedLorry() != null && user.getAssignedLorry().getId().equals(lorry.getId());
            boolean hasTripOnLorry = tripRepository.existsByDriverAndLorry(user, lorry);
            if (!isAssignedLorry && !hasTripOnLorry) {
                log.warn("Access violation: Driver '{}' attempted to access vehicle documents for unassigned lorry ID={}",
                        username, lorry.getId());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lorry not assigned to you.");
            }
        } else {
            if (user.getRole() != Role.OWNER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner role required.");
            }
            if (lorry.getOwner() == null || !lorry.getOwner().getId().equals(user.getId())) {
                log.warn("Access violation: Owner '{}' attempted to access lorry ID={} owned by another owner",
                        username, lorry.getId());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lorry not found in your fleet.");
            }
        }

        VehicleDocument doc = vehicleDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle document not found."));

        if (!doc.getLorry().getId().equals(lorry.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document does not belong to specified lorry.");
        }

        Resource resource = storageService.loadFileAsResource("vehicles", doc.getStoredFilename());
        return new TripDocumentService.DownloadableDocument(resource, doc.getContentType(), doc.getOriginalFilename(), doc.getDocumentName());
    }

    /**
     * Deletes a vehicle document (Owner only).
     */
    @Transactional
    public void deleteDocumentForOwner(Long lorryId, Long documentId, String ownerUsername) {
        User owner = getAuthenticatedUser(ownerUsername, Role.OWNER);
        Lorry lorry = getAuthorizedLorryForOwner(lorryId, owner);

        VehicleDocument doc = vehicleDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle document not found."));

        if (!doc.getLorry().getId().equals(lorry.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document does not belong to specified lorry.");
        }

        storageService.deleteFile("vehicles", doc.getStoredFilename());
        vehicleDocumentRepository.delete(doc);

        log.info("Owner '{}' deleted vehicle document ID={} from lorry ID={}",
                owner.getUsername(), documentId, lorry.getId());
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

    private Lorry getAuthorizedLorryForOwner(Long lorryId, User owner) {
        Lorry lorry = lorryRepository.findById(lorryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lorry not found with ID: " + lorryId));
        if (lorry.getOwner() == null || !lorry.getOwner().getId().equals(owner.getId())) {
            log.warn("Access violation: Owner '{}' attempted to access lorry ID={} not owned by them",
                    owner.getUsername(), lorry.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lorry not found in your fleet.");
        }
        return lorry;
    }

    private Lorry getAuthorizedLorryForDriver(Long lorryId, User driver) {
        Lorry lorry = lorryRepository.findById(lorryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lorry not found with ID: " + lorryId));

        boolean isAssignedLorry = driver.getAssignedLorry() != null && driver.getAssignedLorry().getId().equals(lorry.getId());
        boolean hasTripOnLorry = tripRepository.existsByDriverAndLorry(driver, lorry);

        if (!isAssignedLorry && !hasTripOnLorry) {
            log.warn("Access violation: Driver '{}' attempted to access vehicle documents for unassigned lorry ID={}",
                    driver.getUsername(), lorry.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lorry not assigned to you.");
        }
        return lorry;
    }

    private VehicleDocumentDto mapToDto(VehicleDocument doc, String downloadUrl) {
        return new VehicleDocumentDto(
                doc.getId(),
                doc.getLorry().getId(),
                doc.getDocumentName(),
                doc.getOriginalFilename(),
                doc.getContentType(),
                doc.getFileSize(),
                doc.getUploadedAt(),
                doc.getUploadedBy() != null ? doc.getUploadedBy().getFullName() : null,
                downloadUrl
        );
    }
}
