package com.lorryowner.backend.repository;

import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.OperationalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LorryRepository extends JpaRepository<Lorry, Long> {

    Optional<Lorry> findByRegistrationNumber(String registrationNumber);

    Optional<Lorry> findByRegistrationNumberIgnoreCase(String registrationNumber);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);

    List<Lorry> findByOperationalStatus(OperationalStatus status);

    long countByOperationalStatus(OperationalStatus status);

    long countByOperationalStatusIn(Collection<OperationalStatus> statuses);

    List<Lorry> findAllByOrderByUpdatedAtDesc();

    @Query("SELECT l FROM Lorry l WHERE l.owner = :owner ORDER BY l.updatedAt DESC")
    List<Lorry> findByOwnerOrderByUpdatedAtDesc(@Param("owner") com.lorryowner.backend.entity.User owner);

    @Query("SELECT l FROM Lorry l WHERE l.owner = :owner ORDER BY l.registrationNumber ASC")
    List<Lorry> findByOwnerOrderByRegistrationNumberAsc(@Param("owner") com.lorryowner.backend.entity.User owner);

    @Query("SELECT l FROM Lorry l WHERE l.owner = :owner AND l.operationalStatus = :status ORDER BY l.registrationNumber ASC")
    List<Lorry> findByOwnerAndOperationalStatusOrderByRegistrationNumberAsc(
            @Param("owner") com.lorryowner.backend.entity.User owner,
            @Param("status") OperationalStatus status
    );

    @Query("SELECT COUNT(l) FROM Lorry l WHERE l.owner = :owner")
    long countByOwner(@Param("owner") com.lorryowner.backend.entity.User owner);

    @Query("SELECT COUNT(l) FROM Lorry l WHERE l.owner = :owner AND l.operationalStatus = :status")
    long countByOwnerAndOperationalStatus(
            @Param("owner") com.lorryowner.backend.entity.User owner, 
            @Param("status") OperationalStatus status
    );

    @Query("SELECT COUNT(l) FROM Lorry l WHERE l.owner = :owner AND l.operationalStatus IN :statuses")
    long countByOwnerAndOperationalStatusIn(
            @Param("owner") com.lorryowner.backend.entity.User owner,
            @Param("statuses") Collection<OperationalStatus> statuses
    );

    @Query("SELECT l FROM Lorry l WHERE l.id = :id AND l.owner = :owner")
    Optional<Lorry> findByIdAndOwner(
            @Param("id") Long id, 
            @Param("owner") com.lorryowner.backend.entity.User owner
    );
}
