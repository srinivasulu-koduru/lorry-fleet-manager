package com.lorryowner.backend.repository;

import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.OperationalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LorryRepository extends JpaRepository<Lorry, Long> {

    Optional<Lorry> findByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumber(String registrationNumber);

    List<Lorry> findByOperationalStatus(OperationalStatus status);

    long countByOperationalStatus(OperationalStatus status);

    long countByOperationalStatusIn(Collection<OperationalStatus> statuses);

    List<Lorry> findAllByOrderByUpdatedAtDesc();
}
