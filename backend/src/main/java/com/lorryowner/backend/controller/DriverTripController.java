package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.driver.DriverDashboardResponse;
import com.lorryowner.backend.dto.driver.DriverProfileResponse;
import com.lorryowner.backend.dto.driver.TripDto;
import com.lorryowner.backend.dto.driver.UpdateTripNotesRequest;
import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.OperationalStatus;
import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.Trip;
import com.lorryowner.backend.entity.TripStatus;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.TripRepository;
import com.lorryowner.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Controller providing read endpoints and secure status workflow mutations for authenticated Drivers.
 *
 * Security Architecture:
 * 1. Role-guarded at method & route level: only ROLE_DRIVER can access.
 * 2. Identity derived strictly from validated Spring SecurityContext JWT principal (never client IDs).
 * 3. Enforces strict record-level authorization: drivers can ONLY inspect and mutate trips assigned to them.
 * 4. Enforces strict state machine transitions: PLANNED -> IN_PROGRESS -> COMPLETED.
 */
@RestController
@RequestMapping("/api/driver")
@PreAuthorize("hasRole('DRIVER')")
public class DriverTripController {

    private static final Logger log = LoggerFactory.getLogger(DriverTripController.class);

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final LorryRepository lorryRepository;

    public DriverTripController(
            UserRepository userRepository,
            TripRepository tripRepository,
            LorryRepository lorryRepository
    ) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.lorryRepository = lorryRepository;
    }

    /**
     * Retrieves high-level driver dashboard metrics and today's active/planned trip.
     * GET /api/driver/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DriverDashboardResponse> getDriverDashboard(Authentication authentication) {
        User driver = getAuthenticatedDriver(authentication);

        String assignedLorryReg = driver.getAssignedLorry() != null 
                ? driver.getAssignedLorry().getRegistrationNumber() 
                : null;
        String assignedLorryModel = driver.getAssignedLorry() != null 
                ? driver.getAssignedLorry().getMakeModel() 
                : null;

        long currentCount = tripRepository.countByDriverAndTripStatus(driver, TripStatus.IN_PROGRESS);
        long upcomingCount = tripRepository.countByDriverAndTripStatus(driver, TripStatus.PLANNED);
        long completedCount = tripRepository.countByDriverAndTripStatus(driver, TripStatus.COMPLETED);

        // Find today's relevant trip: prefer active IN_PROGRESS trip, fallback to next PLANNED trip
        Optional<Trip> todayTripOpt = tripRepository.findFirstByDriverAndTripStatusOrderByTripDateDescEndTimeDesc(driver, TripStatus.IN_PROGRESS);
        if (todayTripOpt.isEmpty()) {
            todayTripOpt = tripRepository.findFirstByDriverAndTripStatusOrderByTripDateDescEndTimeDesc(driver, TripStatus.PLANNED);
        }

        TripDto todayTripDto = todayTripOpt.map(this::mapToTripDto).orElse(null);

        DriverDashboardResponse response = new DriverDashboardResponse(
                driver.getFullName(),
                driver.getUsername(),
                assignedLorryReg,
                assignedLorryModel,
                todayTripDto,
                currentCount,
                upcomingCount,
                completedCount
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Lists trips assigned to the authenticated driver, optionally filtered by status.
     * GET /api/driver/trips?status=PLANNED
     */
    @GetMapping("/trips")
    public ResponseEntity<List<TripDto>> getDriverTrips(
            @RequestParam(required = false) String status,
            Authentication authentication
    ) {
        User driver = getAuthenticatedDriver(authentication);

        List<Trip> trips;
        if (status != null && !status.trim().isBlank()) {
            try {
                TripStatus filterStatus = TripStatus.valueOf(status.trim().toUpperCase());
                trips = tripRepository.findByDriverOrderByTripDateDescStartTimeDesc(driver).stream()
                        .filter(t -> t.getTripStatus() == filterStatus)
                        .toList();
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid trip status: " + status);
            }
        } else {
            trips = tripRepository.findByDriverOrderByTripDateDescStartTimeDesc(driver);
        }

        List<TripDto> dtoList = trips.stream().map(this::mapToTripDto).toList();
        return ResponseEntity.ok(dtoList);
    }

    /**
     * Retrieves specific trip details with strict record-level driver ownership check.
     * GET /api/driver/trips/{id}
     */
    @GetMapping("/trips/{id}")
    public ResponseEntity<TripDto> getTripDetails(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User driver = getAuthenticatedDriver(authentication);

        Trip trip = tripRepository.findByIdAndDriverId(id, driver.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, 
                        "Trip not found or you do not have permission to view it."
                ));

        return ResponseEntity.ok(mapToTripDto(trip));
    }

    /**
     * Retrieves profile details for the authenticated driver.
     * GET /api/driver/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<DriverProfileResponse> getDriverProfile(Authentication authentication) {
        User driver = getAuthenticatedDriver(authentication);

        String assignedLorryReg = driver.getAssignedLorry() != null 
                ? driver.getAssignedLorry().getRegistrationNumber() 
                : null;
        String assignedLorryModel = driver.getAssignedLorry() != null 
                ? driver.getAssignedLorry().getMakeModel() 
                : null;

        DriverProfileResponse response = new DriverProfileResponse(
                driver.getId(),
                driver.getUsername(),
                driver.getRole().name(),
                driver.getFullName(),
                driver.getPhoneNumber(),
                driver.getLicenseNumber(),
                assignedLorryReg,
                assignedLorryModel,
                driver.isActive(),
                driver.getCreatedAt()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Starts an assigned planned trip.
     * Transitions trip lifecycle from PLANNED to IN_PROGRESS.
     * POST /api/driver/trips/{id}/start
     */
    @PostMapping("/trips/{id}/start")
    @Transactional
    public ResponseEntity<TripDto> startTrip(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User driver = getAuthenticatedDriver(authentication);

        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + id));

        if (!trip.getDriver().getId().equals(driver.getId())) {
            log.warn("Access violation: Driver '{}' (ID={}) attempted to start trip ID={} belonging to driver ID={}",
                    driver.getUsername(), driver.getId(), trip.getId(), trip.getDriver().getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found or not assigned to you.");
        }

        // Idempotent handling: If already IN_PROGRESS, return current state safely
        if (trip.getTripStatus() == TripStatus.IN_PROGRESS) {
            log.info("Driver '{}' re-requested start on trip '{}' (already IN_PROGRESS); returning idempotent OK",
                    driver.getUsername(), trip.getTripReference());
            return ResponseEntity.ok(mapToTripDto(trip));
        }

        // Validate state transitions
        if (trip.getTripStatus() == TripStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot start a trip that is already completed.");
        }
        if (trip.getTripStatus() == TripStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot start a cancelled trip.");
        }
        if (trip.getTripStatus() != TripStatus.PLANNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trip is not in PLANNED status (current: " + trip.getTripStatus() + ")");
        }

        // Concurrency safeguard: ensure driver doesn't have another simultaneous IN_PROGRESS trip
        boolean hasOtherActive = tripRepository.existsByDriverAndTripStatusIn(driver, List.of(TripStatus.IN_PROGRESS));
        if (hasOtherActive) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active trip in progress. Please complete that trip before starting another.");
        }

        // Perform transition
        trip.setTripStatus(TripStatus.IN_PROGRESS);
        if (trip.getStartTime() == null) {
            trip.setStartTime(Instant.now());
        }

        if (trip.getLorry() != null) {
            Lorry lorry = trip.getLorry();
            lorry.setOperationalStatus(OperationalStatus.ON_TRIP);
            lorryRepository.save(lorry);
        }

        Trip saved = tripRepository.save(trip);
        log.info("Trip '{}' (ID={}) started by Driver '{}'. Status transitioned to IN_PROGRESS.",
                saved.getTripReference(), saved.getId(), driver.getUsername());

        return ResponseEntity.ok(mapToTripDto(saved));
    }

    /**
     * Completes an in-progress trip.
     * Transitions trip lifecycle from IN_PROGRESS to COMPLETED.
     * POST /api/driver/trips/{id}/complete
     */
    @PostMapping(value = "/trips/{id}/complete", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE})
    @Transactional
    public ResponseEntity<TripDto> completeTrip(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateTripNotesRequest request,
            Authentication authentication
    ) {
        User driver = getAuthenticatedDriver(authentication);

        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + id));

        if (!trip.getDriver().getId().equals(driver.getId())) {
            log.warn("Access violation: Driver '{}' (ID={}) attempted to complete trip ID={} belonging to driver ID={}",
                    driver.getUsername(), driver.getId(), trip.getId(), trip.getDriver().getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found or not assigned to you.");
        }

        // Idempotent handling: If already COMPLETED, return current state safely
        if (trip.getTripStatus() == TripStatus.COMPLETED) {
            log.info("Driver '{}' re-requested complete on trip '{}' (already COMPLETED); returning idempotent OK",
                    driver.getUsername(), trip.getTripReference());
            return ResponseEntity.ok(mapToTripDto(trip));
        }

        // Validate state transitions
        if (trip.getTripStatus() == TripStatus.PLANNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot complete a trip that has not been started yet. Trip must be IN_PROGRESS.");
        }
        if (trip.getTripStatus() == TripStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot complete a cancelled trip.");
        }
        if (trip.getTripStatus() != TripStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trip is not in IN_PROGRESS status (current: " + trip.getTripStatus() + ")");
        }

        // Perform transition
        trip.setTripStatus(TripStatus.COMPLETED);
        trip.setEndTime(Instant.now());

        // Append optional completion remarks / delivery notes if provided
        if (request != null && request.notes() != null && !request.notes().trim().isBlank()) {
            String cleanNotes = request.notes().trim();
            String existingNotes = trip.getDeliveryNotes();
            if (existingNotes != null && !existingNotes.isBlank()) {
                trip.setDeliveryNotes(existingNotes + "\n[Completion Note]: " + cleanNotes);
            } else {
                trip.setDeliveryNotes("[Completion Note]: " + cleanNotes);
            }
        }

        // Release lorry back to AVAILABLE if no other trips in progress for this lorry
        if (trip.getLorry() != null) {
            Lorry lorry = trip.getLorry();
            boolean hasOtherInProgress = tripRepository.existsByLorryAndTripStatusIn(lorry, List.of(TripStatus.IN_PROGRESS));
            if (!hasOtherInProgress) {
                lorry.setOperationalStatus(OperationalStatus.AVAILABLE);
                lorryRepository.save(lorry);
            }
        }

        Trip saved = tripRepository.save(trip);
        log.info("Trip '{}' (ID={}) completed by Driver '{}'. Status transitioned to COMPLETED.",
                saved.getTripReference(), saved.getId(), driver.getUsername());

        return ResponseEntity.ok(mapToTripDto(saved));
    }

    private User getAuthenticatedDriver(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found."));

        if (user.getRole() != Role.DRIVER) {
            log.warn("Access denied: User '{}' has role '{}' but tried to access Driver endpoint", username, user.getRole());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Driver role required.");
        }

        return user;
    }

    private TripDto mapToTripDto(Trip trip) {
        return new TripDto(
                trip.getId(),
                trip.getTripReference(),
                trip.getDriver() != null ? trip.getDriver().getId() : null,
                trip.getDriver() != null ? trip.getDriver().getFullName() : null,
                trip.getLorry() != null ? trip.getLorry().getId() : null,
                trip.getLorry() != null ? trip.getLorry().getRegistrationNumber() : "Unknown",
                trip.getLorry() != null ? trip.getLorry().getMakeModel() : null,
                trip.getOriginLocation(),
                trip.getDestinationLocation(),
                trip.getTripStatus() != null ? trip.getTripStatus().name() : "COMPLETED",
                trip.getTripDate(),
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getDistanceKm(),
                trip.getDeliveryNotes(),
                trip.getCreatedAt(),
                trip.getPlannedDeliveryTime(),
                trip.getLoadType()
        );
    }
}
