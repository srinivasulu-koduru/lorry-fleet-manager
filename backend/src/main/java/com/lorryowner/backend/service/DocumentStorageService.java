package com.lorryowner.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service managing secure local filesystem persistence for fleet documents.
 * 
 * Security Features:
 * 1. Safe random UUID generation prevents filename collision & directory traversal.
 * 2. MIME type & extension allowlist enforcement (PDF, JPEG, PNG, WebP only).
 * 3. File size limit enforcement (max 15MB).
 * 4. Path traversal protection: rejects filenames with ".." or directory separators.
 * 5. Zero external subscription or cloud storage cost.
 */
@Service
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

    public static final long MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024; // 15 MB

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "jpg", "jpeg", "png", "webp"
    );

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final Path storageRoot;

    public DocumentStorageService(
            @Value("${app.document.upload-dir:uploads/documents}") String uploadDir
    ) {
        this.storageRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageRoot.resolve("trips"));
            Files.createDirectories(this.storageRoot.resolve("vehicles"));
            log.info("Document storage initialized at: {}", this.storageRoot);
        } catch (IOException e) {
            log.error("Could not create document storage directories", e);
            throw new RuntimeException("Could not initialize document storage", e);
        }
    }

    /**
     * Stores an uploaded multipart file for a trip or vehicle.
     *
     * @param file Uploaded multipart file
     * @param subfolder Subfolder ("trips" or "vehicles")
     * @return Generated safe stored filename (e.g. 550e8400-e29b-41d4-a716-446655440000.pdf)
     */
    public String storeFile(MultipartFile file, String subfolder) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty or missing.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File size exceeds maximum allowed limit of 15MB.");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document"
        );

        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename or path traversal detected.");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported file format: ." + extension + ". Supported formats are PDF, JPG, JPEG, PNG, and WebP.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String lowerType = contentType.toLowerCase();
            boolean isAllowedType = ALLOWED_CONTENT_TYPES.stream().anyMatch(lowerType::startsWith);
            if (!isAllowedType) {
                log.warn("MIME type rejected: {}", contentType);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unsupported MIME type: " + contentType + ". Only PDF and Images are permitted.");
            }
        }

        String storedFilename = UUID.randomUUID().toString() + "." + extension;
        Path targetDir = this.storageRoot.resolve(subfolder).normalize();
        Path targetLocation = targetDir.resolve(storedFilename).normalize();

        // Extra sanity check: ensure target stays within root
        if (!targetLocation.startsWith(this.storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target location.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file successfully: original='{}', stored='{}' in '{}'",
                    originalFilename, storedFilename, subfolder);
            return storedFilename;
        } catch (IOException ex) {
            log.error("Failed to store file: " + originalFilename, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file securely.");
        }
    }

    /**
     * Loads a stored file as a Spring Resource for authenticated download/streaming.
     */
    public Resource loadFileAsResource(String subfolder, String storedFilename) {
        if (storedFilename == null || storedFilename.contains("..") || storedFilename.contains("/") || storedFilename.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename.");
        }

        try {
            Path filePath = this.storageRoot.resolve(subfolder).resolve(storedFilename).normalize();
            if (!filePath.startsWith(this.storageRoot)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Access denied: outside storage root.");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found on storage.");
            }
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file path is invalid.", ex);
        }
    }

    /**
     * Safely deletes a stored file when a document record is deleted.
     */
    public void deleteFile(String subfolder, String storedFilename) {
        if (storedFilename == null || storedFilename.contains("..") || storedFilename.contains("/") || storedFilename.contains("\\")) {
            return;
        }
        try {
            Path filePath = this.storageRoot.resolve(subfolder).resolve(storedFilename).normalize();
            if (filePath.startsWith(this.storageRoot)) {
                Files.deleteIfExists(filePath);
                log.info("Deleted file: {}/{}", subfolder, storedFilename);
            }
        } catch (IOException ex) {
            log.warn("Failed to delete file {}/{}: {}", subfolder, storedFilename, ex.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }
}
