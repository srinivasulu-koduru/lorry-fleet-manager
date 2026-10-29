package com.lorryowner.backend.service;

import com.lorryowner.backend.dto.trip.TripCreateDto;
import com.lorryowner.backend.dto.trip.TripResponseDto;
import com.lorryowner.backend.entity.*;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.TripRepository;
import com.lorryowner.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.List;

@Service
@Transactional
public class OwnerTripService {

    private static final Logger log = LoggerFactory.getLogger(OwnerTripService.class);

    private final TripRepository tripRepository;
    private final LorryRepository lorryRepository;
    private final UserRepository userRepository;

    public OwnerTripService(
            TripRepository tripRepository,
            LorryRepository lorryRepository,
            UserRepository userRepository
    ) {
        this.tripRepository = tripRepository;
        this.lorryRepository = lorryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new planned trip for the authenticated fleet owner.
     * Validates owner authorization, lorry and driver ownership, eligibility,
     * chronological timestamp coherence, and status transitions.
     */
    public TripResponseDto createTrip(TripCreateDto request, String ownerUsername) {
        User owner = resolveAndValidateOwner(ownerUsername);

        // 1. Validate and resolve Lorry
        if (request.lorryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lorry ID is required");
        }
        Lorry lorry = lorryRepository.findById(request.lorryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lorry not found with ID: " + request.lorryId()));

        if (lorry.getOwner() != null && !lorry.getOwner().getId().equals(owner.getId())) {
            log.warn("Security violation: Owner '{}' attempted to create trip on lorry ID={} belonging to owner ID={}",
                    ownerUsername, lorry.getId(), lorry.getOwner().getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-owner access denied: Lorry does not belong to the authenticated owner");
        }

        // 2. Validate and resolve Driver
        if (request.driverId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver ID is required");
        }
        User driver = userRepository.findById(request.driverId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found with ID: " + request.driverId()));

        if (driver.getRole() != Role.DRIVER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected account is not a driver");
        }

        if (driver.getOwner() != null && !driver.getOwner().getId().equals(owner.getId())) {
            log.warn("Security violation: Owner '{}' attempted to create trip with driver ID={} belonging to owner ID={}",
                    ownerUsername, driver.getId(), driver.getOwner().getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-owner access denied: Driver does not belong to the authenticated owner");
        }

        if (!driver.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected driver account is inactive or decommissioned");
        }

        // 3. Validate Origin & Destination
        if (request.origin() == null || request.origin().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Origin location must not be blank");
        }
        if (request.destination() == null || request.destination().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination location must not be blank");
        }
        String cleanOrigin = request.origin().trim();
        String cleanDestination = request.destination().trim();

        if (cleanOrigin.equalsIgnoreCase(cleanDestination)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination must be different from origin");
        }

        // 4. Validate Timestamps
        if (request.plannedDepartureDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Planned departure date is required");
        }
        if (request.plannedDepartureTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Planned departure time is required");
        }

        LocalDateTime departureLdt = request.plannedDepartureDate().atTime(request.plannedDepartureTime());
        Instant departureInstant = departureLdt.atZone(ZoneId.systemDefault()).toInstant();

        // Delivery date & time pairing validation
        boolean hasDeliveryDate = request.plannedDeliveryDate() != null;
        boolean hasDeliveryTime = request.plannedDeliveryTime() != null;

        if (hasDeliveryDate != hasDeliveryTime) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Planned delivery date and planned delivery time must either both be present or both be omitted");
        }

        Instant deliveryInstant = null;
        if (hasDeliveryDate) {
            LocalDateTime deliveryLdt = request.plannedDeliveryDate().atTime(request.plannedDeliveryTime());
            if (!deliveryLdt.isAfter(departureLdt)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Planned delivery time must be later than planned departure time");
            }
            deliveryInstant = deliveryLdt.atZone(ZoneId.systemDefault()).toInstant();
        }

        // 5. Validate Load Type
        if (request.loadType() == null || request.loadType().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Load type is required");
        }
        String cleanLoadType = request.loadType().trim();

        String cleanRemarks = request.remarks() != null && !request.remarks().trim().isEmpty()
                ? request.remarks().trim()
                : null;

        // 6. Generate Unique Trip Reference
        int currentYear = request.plannedDepartureDate().getYear();
        String tripReference = generateTripReference(currentYear);

        // 7. Persist New Trip (Initial status: PLANNED)
        Trip trip = new Trip();
        trip.setTripReference(tripReference);
        trip.setOwner(owner);
        trip.setLorry(lorry);
        trip.setDriver(driver);
        trip.setOriginLocation(cleanOrigin);
        trip.setDestinationLocation(cleanDestination);
        trip.setTripStatus(TripStatus.PLANNED);
        trip.setTripDate(request.plannedDepartureDate());
        trip.setStartTime(departureInstant);
        trip.setPlannedDeliveryTime(deliveryInstant);
        trip.setLoadType(cleanLoadType);
        trip.setDeliveryNotes(cleanRemarks);

        Trip savedTrip = tripRepository.save(trip);
        log.info("Trip created successfully: ID={} Reference='{}' Lorry='{}' Driver='{}' Status=PLANNED by Owner='{}'",
                savedTrip.getId(), savedTrip.getTripReference(), lorry.getRegistrationNumber(), driver.getUsername(), ownerUsername);

        return mapToResponseDto(savedTrip, request);
    }

    /**
     * Retrieves full trip details by ID for the authenticated owner.
     */
    @Transactional(readOnly = true)
    public TripResponseDto getTripById(Long id, String ownerUsername) {
        User owner = resolveAndValidateOwner(ownerUsername);

        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + id));

        if (trip.getOwner() != null && !trip.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Trip belongs to another fleet owner");
        }

        return mapToResponseDto(trip, null);
    }

    private User resolveAndValidateOwner(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated user");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));

        if (user.getRole() != Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access restricted to authenticated fleet owners only");
        }
        return user;
    }

    private synchronized String generateTripReference(int year) {
        List<String> recentRefs = tripRepository.findRecentTripReferences(PageRequest.of(0, 10));
        int maxSeq = 1000;
        String prefix = "TRIP-" + year + "-";
        for (String ref : recentRefs) {
            if (ref != null && ref.startsWith(prefix)) {
                try {
                    int seq = Integer.parseInt(ref.substring(prefix.length()));
                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        String candidate = prefix + String.format("%04d", maxSeq + 1);
        while (tripRepository.existsByTripReference(candidate)) {
            maxSeq++;
            candidate = prefix + String.format("%04d", maxSeq + 1);
        }
        return candidate;
    }

    private TripResponseDto mapToResponseDto(Trip trip, TripCreateDto request) {
        LocalDate depDate = trip.getTripDate();
        LocalTime depTime = null;
        if (trip.getStartTime() != null) {
            depTime = trip.getStartTime().atZone(ZoneId.systemDefault()).toLocalTime();
        } else if (request != null) {
            depTime = request.plannedDepartureTime();
        }

        LocalDate delDate = null;
        LocalTime delTime = null;
        if (trip.getPlannedDeliveryTime() != null) {
            ZonedDateTime zdt = trip.getPlannedDeliveryTime().atZone(ZoneId.systemDefault());
            delDate = zdt.toLocalDate();
            delTime = zdt.toLocalTime();
        } else if (request != null) {
            delDate = request.plannedDeliveryDate();
            delTime = request.plannedDeliveryTime();
        }

        return new TripResponseDto(
                trip.getId(),
                trip.getTripReference(),
                trip.getLorry() != null ? trip.getLorry().getId() : null,
                trip.getLorry() != null ? trip.getLorry().getRegistrationNumber() : "Unknown",
                trip.getLorry() != null ? trip.getLorry().getMakeModel() : null,
                trip.getDriver() != null ? trip.getDriver().getId() : null,
                trip.getDriver() != null ? trip.getDriver().getFullName() : "Unassigned",
                trip.getOriginLocation(),
                trip.getDestinationLocation(),
                depDate,
                depTime,
                delDate,
                delTime,
                trip.getLoadType(),
                trip.getDeliveryNotes(),
                trip.getTripStatus() != null ? trip.getTripStatus().name() : "PLANNED",
                trip.getCreatedAt() != null ? trip.getCreatedAt() : Instant.now()
        );
    }
}
