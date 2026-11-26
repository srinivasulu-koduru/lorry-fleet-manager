package com.lorryowner.backend.repository;

import com.lorryowner.backend.entity.Trip;
import com.lorryowner.backend.entity.TripStatus;
import com.lorryowner.backend.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    default Page<Trip> findFilteredTripsByOwner(
            User owner,
            TripStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        Sort defaultSort = Sort.by(Sort.Order.desc("tripDate"), Sort.Order.desc("id"));
        Specification<Trip> spec = buildTripFilterSpec(
                owner, null, null, status, startDate, endDate, search, true, null, null, null
        );
        return findAll(spec, withDefaultSort(pageable, defaultSort));
    }

    default Page<Trip> findFilteredTripsByLorry(
            com.lorryowner.backend.entity.Lorry lorry,
            TripStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        Sort defaultSort = Sort.by(Sort.Order.desc("tripDate"), Sort.Order.desc("id"));
        Specification<Trip> spec = buildTripFilterSpec(
                null, lorry, null, status, startDate, endDate, search, false, null, null, null
        );
        return findAll(spec, withDefaultSort(pageable, defaultSort));
    }

    default Page<Trip> findFilteredTrips(
            User driver,
            TripStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String lorryReg,
            String origin,
            String destination,
            Pageable pageable
    ) {
        Sort defaultSort = Sort.by(Sort.Order.desc("tripDate"), Sort.Order.desc("startTime"));
        Specification<Trip> spec = buildTripFilterSpec(
                null, null, driver, status, startDate, endDate, null, false, lorryReg, origin, destination
        );
        return findAll(spec, withDefaultSort(pageable, defaultSort));
    }

    /**
     * Builds dynamic filter predicates only for non-null arguments so PostgreSQL never
     * receives untyped null parameters inside LOWER() or CONCAT() expressions.
     */
    private static Specification<Trip> buildTripFilterSpec(
            User owner,
            com.lorryowner.backend.entity.Lorry lorry,
            User driverFilter,
            TripStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            boolean includeLorryInSearch,
            String lorryReg,
            String origin,
            String destination
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (owner != null) {
                predicates.add(cb.equal(root.get("owner"), owner));
            }
            if (lorry != null) {
                predicates.add(cb.equal(root.get("lorry"), lorry));
            }
            if (driverFilter != null) {
                predicates.add(cb.equal(root.get("driver"), driverFilter));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("tripStatus"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("tripDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("tripDate"), endDate));
            }

            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Join<Trip, User> driverJoin = root.join("driver", JoinType.LEFT);
                List<Predicate> orPredicates = new ArrayList<>();
                orPredicates.add(cb.like(cb.lower(root.get("tripReference")), pattern));
                orPredicates.add(cb.like(cb.lower(root.get("originLocation")), pattern));
                orPredicates.add(cb.like(cb.lower(root.get("destinationLocation")), pattern));
                if (includeLorryInSearch) {
                    Join<Trip, com.lorryowner.backend.entity.Lorry> lorryJoin = root.join("lorry", JoinType.LEFT);
                    orPredicates.add(cb.and(
                            cb.isNotNull(root.get("lorry")),
                            cb.like(cb.lower(lorryJoin.get("registrationNumber")), pattern)
                    ));
                }
                orPredicates.add(cb.and(
                        cb.isNotNull(root.get("driver")),
                        cb.like(cb.lower(driverJoin.get("fullName")), pattern)
                ));
                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }

            if (lorryReg != null && !lorryReg.trim().isEmpty()) {
                String pattern = "%" + lorryReg.trim().toLowerCase(Locale.ROOT) + "%";
                Join<Trip, com.lorryowner.backend.entity.Lorry> lorryJoin = root.join("lorry", JoinType.LEFT);
                predicates.add(cb.and(
                        cb.isNotNull(root.get("lorry")),
                        cb.like(cb.lower(lorryJoin.get("registrationNumber")), pattern)
                ));
            }
            if (origin != null && !origin.trim().isEmpty()) {
                String pattern = "%" + origin.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("originLocation")), pattern));
            }
            if (destination != null && !destination.trim().isEmpty()) {
                String pattern = "%" + destination.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("destinationLocation")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Pageable withDefaultSort(Pageable pageable, Sort defaultSort) {
        if (pageable == null || pageable.isUnpaged()) {
            return Pageable.unpaged(defaultSort);
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    boolean existsByTripReference(String tripReference);

    boolean existsByDriverAndLorry(User driver, com.lorryowner.backend.entity.Lorry lorry);

    boolean existsByLorryAndTripStatusIn(com.lorryowner.backend.entity.Lorry lorry, List<TripStatus> statuses);

    boolean existsByDriverAndTripStatusIn(User driver, List<TripStatus> statuses);

    @Query("SELECT t.tripReference FROM Trip t WHERE t.tripReference LIKE 'TRIP-%' ORDER BY t.id DESC")
    List<String> findRecentTripReferences(Pageable pageable);
}
