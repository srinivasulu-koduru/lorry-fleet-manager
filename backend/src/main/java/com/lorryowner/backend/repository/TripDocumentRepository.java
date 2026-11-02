package com.lorryowner.backend.repository;

import com.lorryowner.backend.entity.Trip;
import com.lorryowner.backend.entity.TripDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripDocumentRepository extends JpaRepository<TripDocument, Long> {
    List<TripDocument> findAllByTripOrderByUploadedAtDesc(Trip trip);
    List<TripDocument> findAllByTripIdOrderByUploadedAtDesc(Long tripId);
    long countByTrip(Trip trip);
}
