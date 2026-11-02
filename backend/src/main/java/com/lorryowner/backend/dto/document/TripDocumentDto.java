package com.lorryowner.backend.dto.document;

import java.time.Instant;

/**
 * Data Transfer Object representing a Trip Document's metadata.
 */
public record TripDocumentDto(
        Long id,
        Long tripId,
        String documentName,
        String originalFilename,
        String contentType,
        Long fileSize,
        Instant uploadedAt,
        String uploadedByName,
        String downloadUrl
) {}
