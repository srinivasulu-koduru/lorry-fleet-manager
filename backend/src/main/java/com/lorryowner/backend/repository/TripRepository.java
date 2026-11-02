package com.lorryowner.backend.repository;

import com.lorryowner.backend.entity.Trip;
import com.lorryowner.backend.entity.TripStatus;
import com.lorryowner.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip> {

    List<Trip> findByDriverOrderByTripDateDescStartTimeDesc(User driver);

    Page<Trip> findByDriver(User driver, Pageable pageable);

    Optional<Trip> findByIdAndDriverId(Long id, Long driverId);

    Optional<Trip> findByIdAndOwner(Long id, User owner);

    long countByDriver(User driver);

    long countByDriverAndTripStatus(User driver, TripStatus tripStatus);

    @Query("SELECT COUNT(DISTINCT t.lorry.id) FROM Trip t WHERE t.driver = :driver")
    long countDistinctLorriesByDriver(@Param("driver") User driver);

    @Query("SELECT COALESCE(SUM(t.distanceKm), 0) FROM Trip t WHERE t.driver = :driver AND t.tripStatus = 'COMPLETED'")
    BigDecimal sumDistanceByDriverCompleted(@Param("driver") User driver);

    @Query("SELECT COALESCE(SUM(t.distanceKm), 0) FROM Trip t WHERE t.owner = :owner AND t.tripStatus = 'COMPLETED'")
    BigDecimal sumDistanceByOwnerCompleted(@Param("owner") User owner);

    long countByOwnerAndTripStatus(User owner, TripStatus tripStatus);

    List<Trip> findByOwnerAndTripStatus(User owner, TripStatus tripStatus);

    Optional<Trip> findFirstByDriverAndTripStatusOrderByTripDateDescEndTimeDesc(User driver, TripStatus status);

    List<Trip> findByLorryOrderByTripDateDescStartTimeDesc(com.lorryowner.backend.entity.Lorry lorry);

    long countByLorry(com.lorryowner.backend.entity.Lorry lorry);

    long countByLorryAndTripStatus(com.lorryowner.backend.entity.Lorry lorry, TripStatus tripStatus);

    @Query("SELECT COALESCE(SUM(t.distanceKm), 0) FROM Trip t WHERE t.lorry = :lorry AND t.tripStatus = 'COMPLETED'")
    BigDecimal sumDistanceByLorryCompleted(@Param("lorry") com.lorryowner.backend.entity.Lorry lorry);

    Optional<Trip> findFirstByLorryAndTripStatusOrderByTripDateDescStartTimeDesc(
            com.lorryowner.backend.entity.Lorry lorry, 
            TripStatus status
    );

    Optional<Trip> findFirstByLorryOrderByTripDateDescEndTimeDesc(com.lorryowner.backend.entity.Lorry lorry);

    long countByOwner(User owner);

    @Query("SELECT t FROM Trip t WHERE t.lorry = :lorry " +
            "AND (:status IS NULL OR t.tripStatus = :status) " +
            "AND (:startDate IS NULL OR t.tripDate >= :startDate) " +
            "AND (:endDate IS NULL OR t.tripDate <= :endDate) " +
            "AND (:search IS NULL OR LOWER(t.tripReference) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(t.originLocation) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(t.destinationLocation) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(t.driver.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY t.tripDate DESC, t.startTime DESC")
    Page<Trip> findFilteredTripsByLorry(
            @Param("lorry") com.lorryowner.backend.entity.Lorry lorry,
            @Param("status") TripStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT t FROM Trip t WHERE t.driver = :driver " +
            "AND (:status IS NULL OR t.tripStatus = :status) " +
            "AND (:startDate IS NULL OR t.tripDate >= :startDate) " +
            "AND (:endDate IS NULL OR t.tripDate <= :endDate) " +
            "AND (:lorryReg IS NULL OR LOWER(t.lorry.registrationNumber) LIKE LOWER(CONCAT('%', :lorryReg, '%'))) " +
            "AND (:origin IS NULL OR LOWER(t.originLocation) LIKE LOWER(CONCAT('%', :origin, '%'))) " +
            "AND (:destination IS NULL OR LOWER(t.destinationLocation) LIKE LOWER(CONCAT('%', :destination, '%'))) " +
            "ORDER BY t.tripDate DESC, t.startTime DESC")
    Page<Trip> findFilteredTrips(
            @Param("driver") User driver,
            @Param("status") TripStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("lorryReg") String lorryReg,
            @Param("origin") String origin,
            @Param("destination") String destination,
            Pageable pageable
    );

    boolean existsByTripReference(String tripReference);

    boolean existsByDriverAndLorry(User driver, com.lorryowner.backend.entity.Lorry lorry);

    boolean existsByLorryAndTripStatusIn(com.lorryowner.backend.entity.Lorry lorry, List<TripStatus> statuses);

    boolean existsByDriverAndTripStatusIn(User driver, List<TripStatus> statuses);

    @Query("SELECT t.tripReference FROM Trip t WHERE t.tripReference LIKE 'TRIP-%' ORDER BY t.id DESC")
    List<String> findRecentTripReferences(Pageable pageable);
}
