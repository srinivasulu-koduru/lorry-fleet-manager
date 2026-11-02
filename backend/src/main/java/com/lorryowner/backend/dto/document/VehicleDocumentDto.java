package com.lorryowner.backend.dto.document;

import java.time.Instant;

/**
 * Data Transfer Object representing a Vehicle Document's metadata.
 */
public record VehicleDocumentDto(
        Long id,
        Long lorryId,
        String documentName,
        String originalFilename,
        String contentType,
        Long fileSize,
        Instant uploadedAt,
        String uploadedByName,
        String downloadUrl
) {}
