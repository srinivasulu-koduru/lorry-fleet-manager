package com.lorryowner.backend.repository;

import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.Trip;
import com.lorryowner.backend.entity.TripLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripLocationRepository extends JpaRepository<TripLocation, Long> {

    Optional<TripLocation> findFirstByTripOrderByIdDesc(Trip trip);

    Optional<TripLocation> findFirstByTripIdOrderByIdDesc(Long tripId);

    Optional<TripLocation> findFirstByLorryOrderByIdDesc(Lorry lorry);

    Optional<TripLocation> findFirstByLorryIdOrderByIdDesc(Long lorryId);

    List<TripLocation> findTop50ByTripOrderByIdAsc(Trip trip);

    List<TripLocation> findTop50ByTripIdOrderByIdAsc(Long tripId);

    void deleteAllByTrip(Trip trip);
}
