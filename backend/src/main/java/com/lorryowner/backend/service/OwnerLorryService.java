package com.lorryowner.backend.service;

import com.lorryowner.backend.dto.driver.TripDto;
import com.lorryowner.backend.dto.driver.TripListResponse;
import com.lorryowner.backend.dto.lorry.CreateLorryRequest;
import com.lorryowner.backend.dto.lorry.FleetSummaryDto;
import com.lorryowner.backend.dto.lorry.LorryMaintenanceResponse;
import com.lorryowner.backend.dto.lorry.MaintenanceRecordDto;
import com.lorryowner.backend.dto.lorry.OwnerLorryDetailDto;
import com.lorryowner.backend.entity.*;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.MaintenanceRecordRepository;
import com.lorryowner.backend.repository.TripRepository;
import com.lorryowner.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OwnerLorryService {

    private static final Logger log = LoggerFactory.getLogger(OwnerLorryService.class);

    private final LorryRepository lorryRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public OwnerLorryService(
            LorryRepository lorryRepository,
            UserRepository userRepository,
            TripRepository tripRepository,
            MaintenanceRecordRepository maintenanceRecordRepository
    ) {
        this.lorryRepository = lorryRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    public FleetSummaryDto getFleetSummary(String ownerUsername) {
        User owner = resolveOwner(ownerUsername);

        long totalLorries = lorryRepository.countByOwner(owner);
        long runningLorries = lorryRepository.countByOwnerAndOperationalStatus(owner, OperationalStatus.ON_TRIP);
        long loadingLorries = lorryRepository.countByOwnerAndOperationalStatus(owner, OperationalStatus.LOADING);
        long availableLorries = lorryRepository.countByOwnerAndOperationalStatus(owner, OperationalStatus.AVAILABLE);
        long notReadyLorries = lorryRepository.countByOwnerAndOperationalStatusIn(
                owner,
                List.of(OperationalStatus.MAINTENANCE, OperationalStatus.UNAVAILABLE)
        );
        long notRunningLorries = Math.max(0, totalLorries - runningLorries);

        double runningPercentage = totalLorries > 0
                ? BigDecimal.valueOf((runningLorries * 100.0) / totalLorries)
                        .setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        double notRunningPercentage = totalLorries > 0
                ? BigDecimal.valueOf((notRunningLorries * 100.0) / totalLorries)
                        .setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        double loadingPercentage = totalLorries > 0
                ? BigDecimal.valueOf((loadingLorries * 100.0) / totalLorries)
                        .setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        double availablePercentage = totalLorries > 0
                ? BigDecimal.valueOf((availableLorries * 100.0) / totalLorries)
                        .setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        double notReadyPercentage = totalLorries > 0
                ? BigDecimal.valueOf((notReadyLorries * 100.0) / totalLorries)
                        .setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        long totalTrips = tripRepository.countByOwner(owner);
        BigDecimal totalDistanceKm = tripRepository.sumDistanceByOwnerCompleted(owner);
        BigDecimal totalMaintenanceCost = maintenanceRecordRepository.sumCostByOwner(owner);

        return new FleetSummaryDto(
                totalLorries,
                runningLorries,
                notRunningLorries,
                runningPercentage,
                notRunningPercentage,
                totalTrips,
                totalDistanceKm != null ? totalDistanceKm : BigDecimal.ZERO,
                totalMaintenanceCost != null ? totalMaintenanceCost : BigDecimal.ZERO,
                loadingLorries,
                loadingPercentage,
                availableLorries,
                availablePercentage,
                notReadyLorries,
                notReadyPercentage
        );
    }

    public List<OwnerLorryDetailDto> getOwnerLorries(String ownerUsername, String filter, String search) {
        User owner = resolveOwner(ownerUsername);
        List<Lorry> lorries = lorryRepository.findByOwnerOrderByRegistrationNumberAsc(owner);

        List<OwnerLorryDetailDto> results = new ArrayList<>();
        String normalizedSearch = (search != null && !search.trim().isBlank()) ? search.trim().toLowerCase() : null;
        String normalizedFilter = (filter != null) ? filter.trim().toUpperCase() : "ALL";

        for (Lorry lorry : lorries) {
            OwnerLorryDetailDto dto = buildLorryDetailDto(lorry, owner);

            // Filter logic: ALL, RUNNING, NOT_RUNNING
            if ("RUNNING".equals(normalizedFilter) && !dto.running()) {
                continue;
            }
            if ("NOT_RUNNING".equals(normalizedFilter) && dto.running()) {
                continue;
            }

            // Search logic (registrationNumber, makeModel, driverName)
            if (normalizedSearch != null) {
                boolean matchesReg = dto.registrationNumber() != null && dto.registrationNumber().toLowerCase().contains(normalizedSearch);
                boolean matchesModel = dto.makeModel() != null && dto.makeModel().toLowerCase().contains(normalizedSearch);
                boolean matchesDriver = dto.assignedDriverName() != null && dto.assignedDriverName().toLowerCase().contains(normalizedSearch);
                if (!matchesReg && !matchesModel && !matchesDriver) {
                    continue;
                }
            }

            results.add(dto);
        }

        return results;
    }

    public OwnerLorryDetailDto getLorryDetails(Long lorryId, String ownerUsername) {
        User owner = resolveOwner(ownerUsername);
        Lorry lorry = getLorryEntityWithOwnershipCheck(lorryId, owner);
        return buildLorryDetailDto(lorry, owner);
    }

    public TripListResponse getLorryTrips(
            Long lorryId,
            String status,
            String search,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String ownerUsername
    ) {
        User owner = resolveOwner(ownerUsername);
        Lorry lorry = getLorryEntityWithOwnershipCheck(lorryId, owner);

        TripStatus tripStatus = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                tripStatus = TripStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid trip status: " + status);
            }
        }

        String searchPattern = (search != null && !search.trim().isBlank()) ? search.trim() : null;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));

        Page<Trip> tripPage = tripRepository.findFilteredTripsByLorry(
                lorry,
                tripStatus,
                startDate,
                endDate,
                searchPattern,
                pageable
        );

        List<TripDto> dtoList = tripPage.getContent().stream()
                .map(this::mapToTripDto)
                .toList();

        return new TripListResponse(
                dtoList,
                tripPage.getTotalElements(),
                tripPage.getTotalPages(),
                tripPage.getNumber(),
                tripPage.getSize()
        );
    }

    public LorryMaintenanceResponse getLorryMaintenance(Long lorryId, String ownerUsername) {
        User owner = resolveOwner(ownerUsername);
        Lorry lorry = getLorryEntityWithOwnershipCheck(lorryId, owner);

        List<MaintenanceRecord> records = maintenanceRecordRepository.findByLorryOrderByMaintenanceDateDesc(lorry);
        BigDecimal totalCost = maintenanceRecordRepository.sumCostByLorry(lorry);

        List<MaintenanceRecordDto> dtoList = records.stream()
                .map(r -> new MaintenanceRecordDto(
                        r.getId(),
                        lorry.getId(),
                        lorry.getRegistrationNumber(),
                        r.getMaintenanceDate(),
                        r.getServiceType(),
                        r.getDescription(),
                        r.getCost(),
                        r.getServiceCenter(),
                        r.getOdometerReadingKm(),
                        r.getNextServiceDate()
                ))
                .toList();

        return new LorryMaintenanceResponse(
                lorry.getId(),
                lorry.getRegistrationNumber(),
                lorry.getMakeModel(),
                totalCost != null ? totalCost : BigDecimal.ZERO,
                dtoList
        );
    }

    /**
     * Registers a new commercial fleet lorry under the authenticated owner.
     * Optionally assigns an eligible driver.
     */
    @Transactional
    public OwnerLorryDetailDto createLorry(CreateLorryRequest request, String ownerUsername) {
        User owner = resolveOwner(ownerUsername);

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }

        if (request.registrationNumber() == null || request.registrationNumber().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration number is required.");
        }

        String normalizedReg = request.registrationNumber().trim().toUpperCase();

        if (normalizedReg.length() < 3 || normalizedReg.length() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration number must be between 3 and 20 characters.");
        }

        if (lorryRepository.existsByRegistrationNumberIgnoreCase(normalizedReg)) {
            log.warn("Lorry registration conflict: Registration '{}' already exists in fleet database", normalizedReg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration number '" + normalizedReg + "' is already registered in the system.");
        }

        if (request.makeModel() == null || request.makeModel().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle make and model is required.");
        }

        if (request.capacityTons() == null || request.capacityTons().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload capacity must be a positive number.");
        }

        if (request.capacityTons().compareTo(new BigDecimal("200.0")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload capacity cannot exceed 200 tons.");
        }

        OperationalStatus status = OperationalStatus.AVAILABLE;
        if (request.operationalStatus() != null && !request.operationalStatus().isBlank()) {
            try {
                OperationalStatus parsed = OperationalStatus.valueOf(request.operationalStatus().trim().toUpperCase());
                if (parsed == OperationalStatus.ON_TRIP) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "A newly registered lorry cannot be in ON_TRIP status.");
                }
                status = parsed;
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid operational status: " + request.operationalStatus());
            }
        }

        Lorry lorry = new Lorry();
        lorry.setRegistrationNumber(normalizedReg);
        lorry.setMakeModel(request.makeModel().trim());
        lorry.setCapacityTons(request.capacityTons());
        lorry.setOperationalStatus(status);
        lorry.setOwner(owner);

        if (request.ialertUnitId() != null && !request.ialertUnitId().isBlank()) {
            lorry.setIalertUnitId(request.ialertUnitId().trim());
        }

        if (request.notes() != null && !request.notes().isBlank()) {
            lorry.setNotes(request.notes().trim());
        }

        Lorry savedLorry = lorryRepository.save(lorry);
        log.info("Lorry registered successfully: ID={}, regNumber='{}', owner='{}'",
                savedLorry.getId(), savedLorry.getRegistrationNumber(), owner.getUsername());

        // Handle optional driver assignment
        if (request.assignedDriverId() != null) {
            User driver = userRepository.findById(request.assignedDriverId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected driver not found."));

            if (driver.getRole() != Role.DRIVER) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not a driver.");
            }

            if (driver.getOwner() != null && !driver.getOwner().getId().equals(owner.getId())) {
                log.warn("Cross-owner driver assignment violation: Owner '{}' attempted to assign driver ID={} owned by ID={}",
                        owner.getUsername(), driver.getId(), driver.getOwner().getId());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Cannot assign a driver belonging to another fleet owner.");
            }

            if (!driver.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot assign an inactive driver.");
            }

            // Unassign this lorry from any prior driver
            userRepository.findByAssignedLorry(savedLorry).ifPresent(prevDriver -> {
                if (!prevDriver.getId().equals(driver.getId())) {
                    prevDriver.setAssignedLorry(null);
                    userRepository.save(prevDriver);
                }
            });

            // Assign driver to this new lorry
            driver.setAssignedLorry(savedLorry);
            userRepository.save(driver);
            log.info("Assigned driver '{}' (ID={}) to newly registered lorry '{}' (ID={})",
                    driver.getUsername(), driver.getId(), savedLorry.getRegistrationNumber(), savedLorry.getId());
        }

        return buildLorryDetailDto(savedLorry, owner);
    }

    private OwnerLorryDetailDto buildLorryDetailDto(Lorry lorry, User owner) {
        // Resolve assigned driver
        Optional<User> assignedDriver = userRepository.findByAssignedLorry(lorry);
        Long driverId = assignedDriver.map(User::getId).orElse(null);
        String driverName = assignedDriver.map(User::getFullName).orElse(null);
        String driverPhone = assignedDriver.map(User::getPhoneNumber).orElse(null);

        // Resolve active trip if on trip
        Optional<Trip> activeTrip = tripRepository.findFirstByLorryAndTripStatusOrderByTripDateDescStartTimeDesc(
                lorry,
                TripStatus.IN_PROGRESS
        );

        boolean isRunning = lorry.getOperationalStatus() == OperationalStatus.ON_TRIP || activeTrip.isPresent();

        Long activeTripId = activeTrip.map(Trip::getId).orElse(null);
        String activeTripRef = activeTrip.map(Trip::getTripReference).orElse(null);
        String origin = activeTrip.map(Trip::getOriginLocation).orElse(null);
        String destination = activeTrip.map(Trip::getDestinationLocation).orElse(null);
        Instant startTime = activeTrip.map(Trip::getStartTime).orElse(null);
        Instant plannedDeliveryTime = activeTrip.map(Trip::getPlannedDeliveryTime).orElse(null);

        String etaFormatted = formatEta(plannedDeliveryTime);

        long totalTrips = tripRepository.countByLorry(lorry);
        long completedTrips = tripRepository.countByLorryAndTripStatus(lorry, TripStatus.COMPLETED);
        long inProgressTrips = tripRepository.countByLorryAndTripStatus(lorry, TripStatus.IN_PROGRESS);
        long cancelledTrips = tripRepository.countByLorryAndTripStatus(lorry, TripStatus.CANCELLED);
        BigDecimal totalDist = tripRepository.sumDistanceByLorryCompleted(lorry);
        BigDecimal maintenanceCost = maintenanceRecordRepository.sumCostByLorry(lorry);

        Instant lastTripDate = tripRepository.findFirstByLorryOrderByTripDateDescEndTimeDesc(lorry)
                .map(Trip::getEndTime)
                .orElse(null);

        return new OwnerLorryDetailDto(
                lorry.getId(),
                lorry.getRegistrationNumber(),
                lorry.getMakeModel(),
                lorry.getCapacityTons(),
                lorry.getOperationalStatus(),
                isRunning,
                driverId,
                driverName,
                driverPhone,
                activeTripId,
                activeTripRef,
                origin,
                destination,
                startTime,
                plannedDeliveryTime,
                etaFormatted,
                totalTrips,
                completedTrips,
                inProgressTrips,
                cancelledTrips,
                totalDist != null ? totalDist : BigDecimal.ZERO,
                maintenanceCost != null ? maintenanceCost : BigDecimal.ZERO,
                lastTripDate,
                lorry.getNotes(),
                lorry.getUpdatedAt()
        );
    }

    private String formatEta(Instant plannedDeliveryTime) {
        if (plannedDeliveryTime == null) {
            return "ETA unavailable";
        }
        Instant now = Instant.now();
        if (now.isAfter(plannedDeliveryTime)) {
            return "ETA: Delayed";
        }
        Duration duration = Duration.between(now, plannedDeliveryTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        if (hours > 0) {
            return "ETA: " + hours + "h " + minutes + "m";
        } else {
            return "ETA: " + minutes + "m";
        }
    }

    private Lorry getLorryEntityWithOwnershipCheck(Long lorryId, User owner) {
        Lorry lorry = lorryRepository.findById(lorryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lorry not found."));

        if (lorry.getOwner() != null && !lorry.getOwner().getId().equals(owner.getId())) {
            log.warn("Ownership violation: Owner '{}' attempted to access lorry ID={} owned by ID={}",
                    owner.getUsername(), lorryId, lorry.getOwner().getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lorry not found.");
        }

        return lorry;
    }

    private User resolveOwner(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User session invalid."));
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
