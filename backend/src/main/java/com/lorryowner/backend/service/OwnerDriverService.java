package com.lorryowner.backend.service;

import com.lorryowner.backend.dto.driver.*;
import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.Trip;
import com.lorryowner.backend.entity.TripStatus;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.TripRepository;
import com.lorryowner.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service managing permanent driver accounts, lorry assignments, trip history,
 * and performance analytics for Fleet Owners.
 * Ensures strict ownership verification, password hashing, and conflict prevention.
 */
@Service
public class OwnerDriverService {

    private static final Logger log = LoggerFactory.getLogger(OwnerDriverService.class);

    private final UserRepository userRepository;
    private final LorryRepository lorryRepository;
    private final TripRepository tripRepository;
    private final PasswordEncoder passwordEncoder;

    public OwnerDriverService(
            UserRepository userRepository,
            LorryRepository lorryRepository,
            TripRepository tripRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.lorryRepository = lorryRepository;
        this.tripRepository = tripRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Backward-compatible driver creation without explicit owner username.
     */
    @Transactional
    public DriverResponse createDriver(CreateDriverRequest request) {
        return createDriver(request, null);
    }

    /**
     * Creates a new permanent driver account belonging to the authenticated owner.
     */
    @Transactional
    public DriverResponse createDriver(CreateDriverRequest request, String ownerUsername) {
        if (request.username() == null || request.username().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required.");
        }
        String username = request.username().trim();

        if (userRepository.existsByUsername(username)) {
            log.warn("Driver creation failed: Username '{}' already taken", username);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username '" + username + "' is already in use.");
        }

        if (request.password() == null || request.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long.");
        }

        if (request.fullName() == null || request.fullName().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required.");
        }

        User owner = resolveOwner(ownerUsername);

        User driver = new User(
                username,
                passwordEncoder.encode(request.password()),
                Role.DRIVER,
                request.fullName().trim(),
                request.phoneNumber() != null ? request.phoneNumber().trim() : null,
                request.licenseNumber() != null ? request.licenseNumber().trim() : null
        );
        driver.setOwner(owner);

        // Handle optional lorry assignment
        Lorry assignedLorry = resolveAndValidateLorry(
                request.assignedLorryId(),
                request.assignedLorryRegistration(),
                owner
        );
        if (assignedLorry != null) {
            unassignLorryFromPreviousDriver(assignedLorry, null);
            driver.setAssignedLorry(assignedLorry);
        }

        User saved = userRepository.save(driver);
        log.info("Driver account created successfully: ID={}, username='{}', owner='{}'",
                saved.getId(), saved.getUsername(), owner != null ? owner.getUsername() : "fleet");

        return mapToDriverResponse(saved);
    }

    /**
     * Backward-compatible driver listing.
     */
    @Transactional(readOnly = true)
    public List<DriverResponse> getAllDrivers() {
        return getAllDrivers(null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> getAllDrivers(String ownerUsername) {
        return getAllDrivers(ownerUsername, null, null, null);
    }

    /**
     * Lists all drivers belonging to the authenticated owner with optional search and filters.
     */
    @Transactional(readOnly = true)
    public List<DriverResponse> getAllDrivers(String ownerUsername, String search, String status, String assigned) {
        User owner = resolveOwner(ownerUsername);
        List<User> drivers;
        if (owner != null) {
            drivers = userRepository.findDriversForOwner(Role.DRIVER, owner);
        } else {
            drivers = userRepository.findByRole(Role.DRIVER);
        }

        return drivers.stream()
                .filter(driver -> {
                    // Status filter: ALL, ACTIVE, INACTIVE
                    if ("ACTIVE".equalsIgnoreCase(status) && !driver.isActive()) return false;
                    if ("INACTIVE".equalsIgnoreCase(status) && driver.isActive()) return false;

                    // Assigned filter: ALL, ASSIGNED, UNASSIGNED
                    if ("ASSIGNED".equalsIgnoreCase(assigned) && driver.getAssignedLorry() == null) return false;
                    if ("UNASSIGNED".equalsIgnoreCase(assigned) && driver.getAssignedLorry() != null) return false;

                    // Search filter: full name, username, phone, assigned lorry registration
                    if (search != null && !search.trim().isBlank()) {
                        String query = search.trim().toLowerCase();
                        boolean matchName = driver.getFullName() != null && driver.getFullName().toLowerCase().contains(query);
                        boolean matchUser = driver.getUsername() != null && driver.getUsername().toLowerCase().contains(query);
                        boolean matchPhone = driver.getPhoneNumber() != null && driver.getPhoneNumber().toLowerCase().contains(query);
                        boolean matchLorry = driver.getAssignedLorry() != null && driver.getAssignedLorry().getRegistrationNumber() != null
                                && driver.getAssignedLorry().getRegistrationNumber().toLowerCase().contains(query);

                        if (!matchName && !matchUser && !matchPhone && !matchLorry) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(this::mapToDriverResponse)
                .toList();
    }

    /**
     * Retrieves single driver details with ownership authorization check.
     */
    @Transactional(readOnly = true)
    public DriverResponse getDriverById(Long id, String ownerUsername) {
        User driver = getDriverEntityWithOwnershipCheck(id, ownerUsername);
        return mapToDriverResponse(driver);
    }

    /**
     * Updates permitted driver details (full name, phone, license, lorry assignment).
     */
    @Transactional
    public DriverResponse updateDriver(Long id, UpdateDriverRequest request, String ownerUsername) {
        User driver = getDriverEntityWithOwnershipCheck(id, ownerUsername);
        User owner = resolveOwner(ownerUsername);

        if (request.fullName() != null && !request.fullName().trim().isBlank()) {
            driver.setFullName(request.fullName().trim());
        }
        if (request.phoneNumber() != null) {
            driver.setPhoneNumber(request.phoneNumber().trim().isEmpty() ? null : request.phoneNumber().trim());
        }
        if (request.licenseNumber() != null) {
            driver.setLicenseNumber(request.licenseNumber().trim().isEmpty() ? null : request.licenseNumber().trim());
        }

        // Lorry assignment updates if provided
        if (request.assignedLorryId() != null || (request.assignedLorryRegistration() != null && !request.assignedLorryRegistration().isBlank())) {
            Lorry lorry = resolveAndValidateLorry(request.assignedLorryId(), request.assignedLorryRegistration(), owner);
            if (lorry != null) {
                unassignLorryFromPreviousDriver(lorry, driver.getId());
                driver.setAssignedLorry(lorry);
            }
        }

        User saved = userRepository.save(driver);
        log.info("Driver ID={} updated successfully by owner '{}'", saved.getId(), ownerUsername);
        return mapToDriverResponse(saved);
    }

    /**
     * Explicitly assigns or unassigns a lorry to a driver.
     */
    @Transactional
    public DriverResponse assignLorry(Long driverId, AssignLorryRequest request, String ownerUsername) {
        User driver = getDriverEntityWithOwnershipCheck(driverId, ownerUsername);
        User owner = resolveOwner(ownerUsername);

        if (request == null || (request.lorryId() == null && (request.registrationNumber() == null || request.registrationNumber().trim().isBlank()))) {
            // Unassign lorry
            driver.setAssignedLorry(null);
            log.info("Unassigned lorry from driver ID={}", driverId);
        } else {
            Lorry lorry = resolveAndValidateLorry(request.lorryId(), request.registrationNumber(), owner);
            if (lorry == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified lorry not found.");
            }
            unassignLorryFromPreviousDriver(lorry, driver.getId());
            driver.setAssignedLorry(lorry);
            log.info("Assigned lorry '{}' to driver ID={}", lorry.getRegistrationNumber(), driverId);
        }

        User saved = userRepository.save(driver);
        return mapToDriverResponse(saved);
    }

    /**
     * Toggles driver active/inactive status.
     */
    @Transactional
    public DriverResponse updateDriverStatus(Long driverId, boolean isActive, String ownerUsername) {
        User driver = getDriverEntityWithOwnershipCheck(driverId, ownerUsername);
        driver.setActive(isActive);

        User saved = userRepository.save(driver);
        log.info("Driver ID={} status changed to isActive={} by owner '{}'", driverId, isActive, ownerUsername);
        return mapToDriverResponse(saved);
    }

    /**
     * Deletes or safely deactivates a driver.
     */
    @Transactional
    public DriverResponse deleteDriver(Long driverId, String ownerUsername) {
        User driver = getDriverEntityWithOwnershipCheck(driverId, ownerUsername);

        // First unassign any active lorry
        driver.setAssignedLorry(null);
        driver.setActive(false);
        userRepository.save(driver);

        try {
            userRepository.delete(driver);
            userRepository.flush();
            log.info("Driver ID={} permanently deleted by owner '{}'", driverId, ownerUsername);
            return mapToDriverResponse(driver);
        } catch (Exception ex) {
            log.warn("Driver ID={} cannot be hard-deleted due to existing history/constraints; softly deactivated.", driverId);
            return mapToDriverResponse(driver);
        }
    }

    // --- Driver Trip History and Performance Analytics ---

    /**
     * Retrieves filtered and paginated trip history for a driver with strict owner verification.
     */
    @Transactional(readOnly = true)
    public TripListResponse getDriverTrips(
            Long driverId,
            String ownerUsername,
            TripStatus status,
            LocalDate startDate,
            LocalDate endDate,
            String lorryReg,
            String origin,
            String destination,
            int page,
            int size
    ) {
        User driver = getDriverEntityWithOwnershipCheck(driverId, ownerUsername);

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)));
        Page<Trip> tripPage = tripRepository.findFilteredTrips(
                driver,
                status,
                startDate,
                endDate,
                lorryReg != null && !lorryReg.trim().isBlank() ? lorryReg.trim() : null,
                origin != null && !origin.trim().isBlank() ? origin.trim() : null,
                destination != null && !destination.trim().isBlank() ? destination.trim() : null,
                pageable
        );

        List<TripDto> tripDtos = tripPage.getContent().stream()
                .map(this::mapToTripDto)
                .toList();

        return new TripListResponse(
                tripDtos,
                tripPage.getTotalElements(),
                tripPage.getTotalPages(),
                tripPage.getNumber(),
                tripPage.getSize()
        );
    }

    /**
     * Retrieves single trip detail with driver and owner validation.
     */
    @Transactional(readOnly = true)
    public TripDto getDriverTripById(Long driverId, Long tripId, String ownerUsername) {
        User driver = getDriverEntityWithOwnershipCheck(driverId, ownerUsername);
        Trip trip = tripRepository.findByIdAndDriverId(tripId, driver.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found."));

        return mapToTripDto(trip);
    }

    /**
     * Calculates real driver performance summary metrics from persisted trip records.
     */
    @Transactional(readOnly = true)
    public DriverPerformanceDto getDriverPerformance(Long driverId, String ownerUsername) {
        User driver = getDriverEntityWithOwnershipCheck(driverId, ownerUsername);

        long totalTrips = tripRepository.countByDriver(driver);
        long completedTrips = tripRepository.countByDriverAndTripStatus(driver, TripStatus.COMPLETED);
        long inProgressTrips = tripRepository.countByDriverAndTripStatus(driver, TripStatus.IN_PROGRESS);
        long cancelledTrips = tripRepository.countByDriverAndTripStatus(driver, TripStatus.CANCELLED);
        BigDecimal distance = tripRepository.sumDistanceByDriverCompleted(driver);
        long distinctLorries = tripRepository.countDistinctLorriesByDriver(driver);

        Optional<Trip> recentTrip = tripRepository.findFirstByDriverAndTripStatusOrderByTripDateDescEndTimeDesc(
                driver, TripStatus.COMPLETED
        );

        return new DriverPerformanceDto(
                driver.getId(),
                driver.getUsername(),
                driver.getFullName(),
                totalTrips,
                completedTrips,
                inProgressTrips,
                cancelledTrips,
                distance != null ? distance : BigDecimal.ZERO,
                distinctLorries,
                recentTrip.map(Trip::getTripDate).orElse(null),
                recentTrip.map(Trip::getTripReference).orElse(null)
        );
    }

    // --- Internal Helpers ---

    private User resolveOwner(String ownerUsername) {
        if (ownerUsername == null || ownerUsername.isBlank()) {
            return null;
        }
        return userRepository.findByUsername(ownerUsername).orElse(null);
    }

    private User getDriverEntityWithOwnershipCheck(Long id, String ownerUsername) {
        User driver = userRepository.findByIdAndRole(id, Role.DRIVER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found."));

        if (ownerUsername != null && !ownerUsername.isBlank()) {
            User owner = userRepository.findByUsername(ownerUsername).orElse(null);
            if (owner != null && driver.getOwner() != null && !driver.getOwner().getId().equals(owner.getId())) {
                log.warn("Security violation: Owner '{}' attempted to access driver ID={} owned by ID={}",
                        ownerUsername, id, driver.getOwner().getId());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found.");
            }
        }

        return driver;
    }

    private Lorry resolveAndValidateLorry(Long lorryId, String registrationNumber, User owner) {
        Lorry lorry = null;
        if (lorryId != null) {
            lorry = lorryRepository.findById(lorryId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lorry with ID " + lorryId + " not found."));
        } else if (registrationNumber != null && !registrationNumber.trim().isBlank()) {
            String reg = registrationNumber.trim();
            lorry = lorryRepository.findByRegistrationNumberIgnoreCase(reg)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lorry with registration '" + reg + "' not found."));
        }

        if (lorry != null && owner != null && lorry.getOwner() != null) {
            if (!lorry.getOwner().getId().equals(owner.getId())) {
                log.warn("Ownership violation: Owner '{}' tried to assign lorry '{}' owned by another owner",
                        owner.getUsername(), lorry.getRegistrationNumber());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to assign this lorry.");
            }
        }

        return lorry;
    }

    private void unassignLorryFromPreviousDriver(Lorry lorry, Long currentDriverId) {
        userRepository.findByAssignedLorry(lorry).ifPresent(previousDriver -> {
            if (currentDriverId == null || !previousDriver.getId().equals(currentDriverId)) {
                log.info("Unassigning lorry '{}' from previous driver ID={}", lorry.getRegistrationNumber(), previousDriver.getId());
                previousDriver.setAssignedLorry(null);
                userRepository.save(previousDriver);
            }
        });
    }

    private DriverResponse mapToDriverResponse(User user) {
        String assignedLorryReg = user.getAssignedLorry() != null ? user.getAssignedLorry().getRegistrationNumber() : null;
        Long assignedLorryId = user.getAssignedLorry() != null ? user.getAssignedLorry().getId() : null;
        String ownerUsername = user.getOwner() != null ? user.getOwner().getUsername() : null;

        long completedTrips = tripRepository.countByDriverAndTripStatus(user, TripStatus.COMPLETED);
        BigDecimal distance = tripRepository.sumDistanceByDriverCompleted(user);

        return new DriverResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getLicenseNumber(),
                user.isActive(),
                user.getCreatedAt(),
                assignedLorryReg,
                assignedLorryId,
                ownerUsername,
                completedTrips,
                distance != null ? distance : BigDecimal.ZERO
        );
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
