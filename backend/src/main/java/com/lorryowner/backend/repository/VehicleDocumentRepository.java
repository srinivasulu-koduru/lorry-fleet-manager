package com.lorryowner.backend.repository;

import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {
    List<VehicleDocument> findAllByLorryOrderByUploadedAtDesc(Lorry lorry);
    List<VehicleDocument> findAllByLorryIdOrderByUploadedAtDesc(Long lorryId);
    long countByLorry(Lorry lorry);
}
