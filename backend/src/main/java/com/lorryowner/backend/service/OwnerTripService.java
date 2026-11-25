package com.lorryowner.backend.service;

import com.lorryowner.backend.dto.driver.TripDto;
import com.lorryowner.backend.dto.driver.TripListResponse;
import com.lorryowner.backend.dto.finance.FinanceTransactionDto;
import com.lorryowner.backend.dto.lorry.MaintenanceRecordDto;
import com.lorryowner.backend.dto.maintenance.CreateMaintenanceRecordRequest;
import com.lorryowner.backend.dto.trip.CreateTripExpenseRequest;
import com.lorryowner.backend.dto.trip.TripCreateDto;
import com.lorryowner.backend.dto.trip.TripDetailResponseDto;
import com.lorryowner.backend.dto.trip.TripResponseDto;
import com.lorryowner.backend.entity.*;
import com.lorryowner.backend.repository.FinanceTransactionRepository;
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
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OwnerTripService {

    private static final Logger log = LoggerFactory.getLogger(OwnerTripService.class);

    private final TripRepository tripRepository;
    private final LorryRepository lorryRepository;
    private final UserRepository userRepository;
    private final FinanceTransactionRepository financeTransactionRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public OwnerTripService(
            TripRepository tripRepository,
            LorryRepository lorryRepository,
            UserRepository userRepository,
            FinanceTransactionRepository financeTransactionRepository,
            MaintenanceRecordRepository maintenanceRecordRepository
    ) {
        this.tripRepository = tripRepository;
        this.lorryRepository = lorryRepository;
        this.userRepository = userRepository;
        this.financeTransactionRepository = financeTransactionRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    /**
     * Creates a new planned trip for the authenticated fleet owner.
     * Validates owner authorization, lorry and driver ownership, eligibility,
     * availability (concurrency check against active trips), and amount.
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

        // Concurrency check: Ensure lorry is not already assigned to an active in-progress trip
        if (tripRepository.existsByLorryAndTripStatusIn(lorry, List.of(TripStatus.IN_PROGRESS))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This lorry is already assigned to an active trip. Please select another lorry.");
        }

        if (lorry.getOperationalStatus() != null && lorry.getOperationalStatus() != OperationalStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This lorry is currently " + lorry.getOperationalStatus() + " and not available for new trips. Please select another lorry.");
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

        // Concurrency check: Ensure driver is not already assigned to an active in-progress trip
        if (tripRepository.existsByDriverAndTripStatusIn(driver, List.of(TripStatus.IN_PROGRESS))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This driver is already assigned to an active trip. Please select another driver.");
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

        // 6. Validate Trip Amount (must be non-negative)
        BigDecimal tripAmount = request.tripAmount() != null ? request.tripAmount() : BigDecimal.ZERO;
        if (tripAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trip amount must be non-negative");
        }

        // 7. Generate Unique Trip Reference
        int currentYear = request.plannedDepartureDate().getYear();
        String tripReference = generateTripReference(currentYear);

        // 8. Persist New Trip (Initial status: PLANNED)
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
        trip.setTripAmount(tripAmount);

        Trip savedTrip = tripRepository.save(trip);
        log.info("Trip created successfully: ID={} Reference='{}' Lorry='{}' Driver='{}' Amount={} Status=PLANNED by Owner='{}'",
                savedTrip.getId(), savedTrip.getTripReference(), lorry.getRegistrationNumber(), driver.getUsername(), tripAmount, ownerUsername);

        return mapToResponseDto(savedTrip, request);
    }

    /**
     * Starts an assigned planned trip on behalf of the owner.
     * Transitions trip lifecycle from PLANNED to IN_PROGRESS.
     */
    public TripResponseDto startTrip(Long tripId, String ownerUsername) {
        User owner = resolveAndValidateOwner(ownerUsername);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + tripId));

        if (trip.getOwner() != null && !trip.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Trip belongs to another fleet owner");
        }

        if (trip.getTripStatus() == TripStatus.IN_PROGRESS) {
            return mapToResponseDto(trip, null);
        }
        if (trip.getTripStatus() != TripStatus.PLANNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot start trip with status: " + trip.getTripStatus());
        }

        // Concurrency safeguard
        if (trip.getLorry() != null && tripRepository.existsByLorryAndTripStatusIn(trip.getLorry(), List.of(TripStatus.IN_PROGRESS))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This lorry is already assigned to an active trip. Please select another lorry.");
        }
        if (trip.getDriver() != null && tripRepository.existsByDriverAndTripStatusIn(trip.getDriver(), List.of(TripStatus.IN_PROGRESS))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This driver is already assigned to an active trip. Please select another driver.");
        }

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
        log.info("Owner '{}' started trip ID={} (ref: {}). Lorry set to ON_TRIP.",
                ownerUsername, saved.getId(), saved.getTripReference());

        return mapToResponseDto(saved, null);
    }

    /**
     * Completes an in-progress trip on behalf of the owner.
     * Transitions trip lifecycle from IN_PROGRESS to COMPLETED, releases lorry, and records revenue.
     */
    public TripResponseDto completeTrip(Long tripId, String notes, String ownerUsername) {
        User owner = resolveAndValidateOwner(ownerUsername);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + tripId));

        if (trip.getOwner() != null && !trip.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Trip belongs to another fleet owner");
        }

        if (trip.getTripStatus() == TripStatus.COMPLETED) {
            return mapToResponseDto(trip, null);
        }
        if (trip.getTripStatus() != TripStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trip is not in IN_PROGRESS status (current: " + trip.getTripStatus() + ")");
        }

        trip.setTripStatus(TripStatus.COMPLETED);
        trip.setEndTime(Instant.now());

        if (notes != null && !notes.trim().isBlank()) {
            String cleanNotes = notes.trim();
            String existingNotes = trip.getDeliveryNotes();
            if (existingNotes != null && !existingNotes.isBlank()) {
                trip.setDeliveryNotes(existingNotes + "\n[Completion Note]: " + cleanNotes);
            } else {
                trip.setDeliveryNotes("[Completion Note]: " + cleanNotes);
            }
        }

        Trip saved = tripRepository.saveAndFlush(trip);

        // Release lorry back to AVAILABLE if no other trips in progress for this lorry
        if (saved.getLorry() != null) {
            Lorry lorry = saved.getLorry();
            boolean hasOtherInProgress = tripRepository.existsByLorryAndTripStatusIn(lorry, List.of(TripStatus.IN_PROGRESS));
            if (!hasOtherInProgress) {
                lorry.setOperationalStatus(OperationalStatus.AVAILABLE);
                lorryRepository.save(lorry);
            }
        }

        // Record trip income in finance module if amount > 0 and not already recorded
        recordTripIncomeIfApplicable(saved);

        log.info("Owner '{}' completed trip ID={} (ref: {}). Lorry released to AVAILABLE.",
                ownerUsername, saved.getId(), saved.getTripReference());

        return mapToResponseDto(saved, null);
    }

    /**
     * Records trip amount as income in finance transactions if not already recorded.
     */
    private void recordTripIncomeIfApplicable(Trip trip) {
        if (trip.getTripAmount() != null && trip.getTripAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (!financeTransactionRepository.existsByTripAndTransactionType(trip, "INCOME")) {
                FinanceTransaction ft = new FinanceTransaction(
                        trip.getOwner(),
                        trip.getLorry(),
                        trip,
                        "INCOME",
                        "FREIGHT",
                        "Freight revenue for completed trip " + trip.getTripReference(),
                        trip.getTripAmount(),
                        trip.getTripDate() != null ? trip.getTripDate() : LocalDate.now(),
                        "RECEIVED",
                        "BANK_TRANSFER",
                        trip.getTripReference()
                );
                financeTransactionRepository.save(ft);
                log.info("Recorded trip income of ₹{} for completed trip '{}'",
                        trip.getTripAmount(), trip.getTripReference());
            }
        }
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

    /**
     * Cancels a planned trip for the authenticated owner.
     * Only trips in PLANNED status can be cancelled.
     */
    public TripResponseDto cancelTrip(Long tripId, String ownerUsername) {
        User owner = resolveAndValidateOwner(ownerUsername);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + tripId));

        if (trip.getOwner() != null && !trip.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Trip belongs to another fleet owner");
        }

        if (trip.getTripStatus() != TripStatus.PLANNED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot cancel trip with status: " + trip.getTripStatus() + ". Only PLANNED trips can be cancelled."
            );
        }

        trip.setTripStatus(TripStatus.CANCELLED);
        Trip saved = tripRepository.save(trip);

        log.info("Owner '{}' successfully cancelled planned trip ID={} (ref: {})",
                ownerUsername, saved.getId(), saved.getTripReference());

        return mapToResponseDto(saved, null);
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

        BigDecimal amount = trip.getTripAmount() != null ? trip.getTripAmount() : BigDecimal.ZERO;

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
                trip.getCreatedAt() != null ? trip.getCreatedAt() : Instant.now(),
                amount
        );
    }

    /**
     * Retrieves paginated owner trip history with optional status and search filtering.
     * GET /api/owner/trips
     */
    @Transactional(readOnly = true)
    public TripListResponse getOwnerTrips(
            String statusStr,
            String search,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String ownerUsername
    ) {
        User owner = resolveAndValidateOwner(ownerUsername);

        TripStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr.trim())) {
            try {
                status = TripStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid trip status: " + statusStr);
            }
        }

        int pageNum = Math.max(0, page);
        int pageSize = (size > 0 && size <= 100) ? size : 20;
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<Trip> tripsPage = tripRepository.findFilteredTripsByOwner(
                owner, status, startDate, endDate, trimmedSearch, pageable
        );

        List<TripDto> dtos = tripsPage.getContent().stream()
                .map(this::mapToTripDto)
                .toList();

        return new TripListResponse(
                dtos,
                tripsPage.getTotalElements(),
                tripsPage.getTotalPages(),
                tripsPage.getNumber(),
                tripsPage.getSize()
        );
    }

    /**
     * Retrieves complete trip details including financial breakdown (revenue, itemized maintenance,
     * fuel, driver, other expenses, total expenses, net profit).
     * GET /api/owner/trips/{id}/details
     */
    @Transactional(readOnly = true)
    public TripDetailResponseDto getTripDetails(Long tripId, String ownerUsername) {
        User owner = resolveAndValidateOwner(ownerUsername);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + tripId));

        if (trip.getOwner() != null && !trip.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Trip belongs to another fleet owner");
        }

        // 1. Fetch trip maintenance records
        List<MaintenanceRecord> mrList = maintenanceRecordRepository.findByTripOrderByMaintenanceDateDesc(trip);
        BigDecimal maintExpenses = BigDecimal.ZERO;
        List<MaintenanceRecordDto> mrDtos = new ArrayList<>(mrList.size());
        for (MaintenanceRecord mr : mrList) {
            maintExpenses = maintExpenses.add(mr.getCost() != null ? mr.getCost() : BigDecimal.ZERO);
            mrDtos.add(new MaintenanceRecordDto(
                    mr.getId(),
                    trip.getLorry() != null ? trip.getLorry().getId() : null,
                    trip.getLorry() != null ? trip.getLorry().getRegistrationNumber() : "Fleet Lorry",
                    mr.getMaintenanceDate(),
                    mr.getServiceType(),
                    mr.getDescription(),
                    mr.getCost(),
                    mr.getServiceCenter(),
                    mr.getOdometerReadingKm(),
                    mr.getNextServiceDate(),
                    trip.getId(),
                    trip.getTripReference()
            ));
        }

        // 2. Fetch other trip expense transactions
        List<FinanceTransaction> ftList = financeTransactionRepository.findByTripAndTransactionTypeOrderByTransactionDateDesc(trip, "EXPENSE");
        BigDecimal fuelExpenses = BigDecimal.ZERO;
        BigDecimal driverExpenses = BigDecimal.ZERO;
        BigDecimal otherExpenses = BigDecimal.ZERO;
        List<FinanceTransactionDto> ftDtos = new ArrayList<>(ftList.size());

        for (FinanceTransaction ft : ftList) {
            BigDecimal amt = ft.getAmount() != null ? ft.getAmount() : BigDecimal.ZERO;
            String cat = ft.getCategory() != null ? ft.getCategory().toUpperCase() : "OTHER";
            if ("DIESEL".equals(cat) || "FUEL".equals(cat)) {
                fuelExpenses = fuelExpenses.add(amt);
            } else if ("DRIVER_SALARY".equals(cat) || "ADVANCE".equals(cat) || "DRIVER".equals(cat)) {
                driverExpenses = driverExpenses.add(amt);
            } else {
                otherExpenses = otherExpenses.add(amt);
            }

            ftDtos.add(new FinanceTransactionDto(
                    ft.getId(),
                    trip.getLorry() != null ? trip.getLorry().getId() : null,
                    trip.getLorry() != null ? trip.getLorry().getRegistrationNumber() : "Fleet Lorry",
                    trip.getId(),
                    trip.getTripReference(),
                    ft.getTransactionType(),
                    ft.getCategory(),
                    ft.getDescription(),
                    ft.getAmount(),
                    ft.getTransactionDate(),
                    ft.getPaymentStatus(),
                    ft.getPaymentMode(),
                    ft.getReferenceNumber()
            ));
        }

        BigDecimal totalExpenses = maintExpenses.add(fuelExpenses).add(driverExpenses).add(otherExpenses);
        BigDecimal tripRevenue = trip.getTripAmount() != null ? trip.getTripAmount() : BigDecimal.ZERO;
        BigDecimal netProfit = tripRevenue.subtract(totalExpenses);

        LocalDate depDate = trip.getTripDate();
        LocalTime depTime = trip.getStartTime() != null
                ? trip.getStartTime().atZone(ZoneId.systemDefault()).toLocalTime()
                : null;

        LocalDate delDate = null;
        LocalTime delTime = null;
        if (trip.getPlannedDeliveryTime() != null) {
            ZonedDateTime zdt = trip.getPlannedDeliveryTime().atZone(ZoneId.systemDefault());
            delDate = zdt.toLocalDate();
            delTime = zdt.toLocalTime();
        }

        return new TripDetailResponseDto(
                trip.getId(),
                trip.getTripReference(),
                trip.getLorry() != null ? trip.getLorry().getId() : null,
                trip.getLorry() != null ? trip.getLorry().getRegistrationNumber() : "Unknown",
                trip.getLorry() != null ? trip.getLorry().getMakeModel() : null,
                trip.getDriver() != null ? trip.getDriver().getId() : null,
                trip.getDriver() != null ? trip.getDriver().getFullName() : "Unassigned",
                trip.getDriver() != null ? trip.getDriver().getPhoneNumber() : null,
                trip.getOriginLocation(),
                trip.getDestinationLocation(),
                depDate,
                depTime,
                trip.getStartTime(),
                delDate,
                delTime,
                trip.getEndTime(),
                trip.getLoadType(),
                trip.getDeliveryNotes(),
                trip.getTripStatus() != null ? trip.getTripStatus().name() : "PLANNED",
                trip.getDistanceKm() != null ? trip.getDistanceKm() : BigDecimal.ZERO,
                trip.getCreatedAt(),
                tripRevenue,
                maintExpenses,
                fuelExpenses,
                driverExpenses,
                otherExpenses,
                totalExpenses,
                netProfit,
                mrDtos,
                ftDtos
        );
    }

    /**
     * Adds a maintenance record specifically linked to a trip.
     * AUTOMATIC LORRY LINKING: Lorry ID is automatically derived from the trip.
     * POST /api/owner/trips/{id}/maintenance
     */
    public MaintenanceRecordDto addTripMaintenance(Long tripId, CreateMaintenanceRecordRequest request, String ownerUsername) {
        User owner = resolveAndValidateOwner(ownerUsername);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + tripId));

        if (trip.getOwner() != null && !trip.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Trip belongs to another fleet owner");
        }

        // Automatic Lorry Linking
        Lorry lorry = trip.getLorry();
        if (lorry == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trip is not associated with any lorry");
        }

        // Security check: validate matching lorry if provided
        if (request.lorryId() != null && !request.lorryId().equals(lorry.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mismatched lorry ID: Provided lorry does not match the trip's assigned lorry (" + lorry.getRegistrationNumber() + ")");
        }

        MaintenanceRecord record = new MaintenanceRecord(
                lorry,
                owner,
                request.maintenanceDate() != null ? request.maintenanceDate() : LocalDate.now(),
                request.serviceType(),
                request.description(),
                request.cost(),
                request.serviceCenter(),
                request.odometerReadingKm() != null ? request.odometerReadingKm() : 0,
                request.nextServiceDate(),
                trip
        );

        MaintenanceRecord saved = maintenanceRecordRepository.save(record);
        log.info("Owner '{}' logged trip maintenance ID={} for Trip ID={} and Lorry ID={}",
                ownerUsername, saved.getId(), trip.getId(), lorry.getId());

        return new MaintenanceRecordDto(
                saved.getId(),
                lorry.getId(),
                lorry.getRegistrationNumber(),
                saved.getMaintenanceDate(),
                saved.getServiceType(),
                saved.getDescription(),
                saved.getCost(),
                saved.getServiceCenter(),
                saved.getOdometerReadingKm(),
                saved.getNextServiceDate(),
                trip.getId(),
                trip.getTripReference()
        );
    }

    /**
     * Adds an expense or maintenance entry inside a trip.
     * Automatically derives the lorry from the trip.
     * If category represents vehicle maintenance, saves into MaintenanceRecord.
     * Otherwise saves into FinanceTransaction (EXPENSE).
     * POST /api/owner/trips/{id}/expenses
     */
    public Object addTripExpense(Long tripId, CreateTripExpenseRequest request, String ownerUsername) {
        User owner = resolveAndValidateOwner(ownerUsername);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + tripId));

        if (trip.getOwner() != null && !trip.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Trip belongs to another fleet owner");
        }

        Lorry lorry = trip.getLorry();
        if (lorry == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trip is not associated with any lorry");
        }

        LocalDate date = request.expenseDate() != null ? request.expenseDate() : LocalDate.now();
        String cat = request.category() != null ? request.category().trim() : "OTHER";

        boolean isMaintenance = isMaintenanceCategory(cat);

        if (isMaintenance) {
            MaintenanceRecord record = new MaintenanceRecord(
                    lorry,
                    owner,
                    date,
                    cat,
                    request.description(),
                    request.amount(),
                    request.serviceCenter() != null ? request.serviceCenter() : request.notes(),
                    request.odometerReadingKm() != null ? request.odometerReadingKm() : 0,
                    null,
                    trip
            );
            MaintenanceRecord saved = maintenanceRecordRepository.save(record);
            log.info("Owner '{}' logged trip maintenance ID={} for Trip ID={} and Lorry ID={}",
                    ownerUsername, saved.getId(), trip.getId(), lorry.getId());

            return new MaintenanceRecordDto(
                    saved.getId(),
                    lorry.getId(),
                    lorry.getRegistrationNumber(),
                    saved.getMaintenanceDate(),
                    saved.getServiceType(),
                    saved.getDescription(),
                    saved.getCost(),
                    saved.getServiceCenter(),
                    saved.getOdometerReadingKm(),
                    saved.getNextServiceDate(),
                    trip.getId(),
                    trip.getTripReference()
            );
        } else {
            String normalizedCategory = normalizeFinanceCategory(cat);
            FinanceTransaction ft = new FinanceTransaction(
                    owner,
                    lorry,
                    trip,
                    "EXPENSE",
                    normalizedCategory,
                    request.description(),
                    request.amount(),
                    date,
                    "PAID",
                    "CASH",
                    request.notes()
            );
            FinanceTransaction saved = financeTransactionRepository.save(ft);
            log.info("Owner '{}' logged trip expense ID={} for Trip ID={} and Lorry ID={}",
                    ownerUsername, saved.getId(), trip.getId(), lorry.getId());

            return new FinanceTransactionDto(
                    saved.getId(),
                    lorry.getId(),
                    lorry.getRegistrationNumber(),
                    trip.getId(),
                    trip.getTripReference(),
                    saved.getTransactionType(),
                    saved.getCategory(),
                    saved.getDescription(),
                    saved.getAmount(),
                    saved.getTransactionDate(),
                    saved.getPaymentStatus(),
                    saved.getPaymentMode(),
                    saved.getReferenceNumber()
            );
        }
    }

    /**
     * Deletes a maintenance record associated with a trip.
     * DELETE /api/owner/trips/{tripId}/maintenance/{maintenanceId}
     */
    public void deleteTripMaintenance(Long tripId, Long maintenanceId, String ownerUsername) {
        User owner = resolveAndValidateOwner(ownerUsername);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + tripId));

        if (trip.getOwner() != null && !trip.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Trip belongs to another fleet owner");
        }

        MaintenanceRecord record = maintenanceRecordRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance record not found with ID: " + maintenanceId));

        if (record.getTrip() == null || !record.getTrip().getId().equals(trip.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maintenance record does not belong to this trip");
        }

        if (record.getOwner() != null && !record.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Record belongs to another fleet owner");
        }

        maintenanceRecordRepository.delete(record);
        log.info("Audit: Owner '{}' deleted maintenance record ID={} for Trip ID={}", ownerUsername, maintenanceId, tripId);
    }

    /**
     * Deletes an expense transaction associated with a trip.
     * DELETE /api/owner/trips/{tripId}/expenses/{expenseId}
     */
    public void deleteTripExpense(Long tripId, Long expenseId, String ownerUsername) {
        User owner = resolveAndValidateOwner(ownerUsername);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found with ID: " + tripId));

        if (trip.getOwner() != null && !trip.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Trip belongs to another fleet owner");
        }

        FinanceTransaction ft = financeTransactionRepository.findById(expenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense transaction not found with ID: " + expenseId));

        if (ft.getTrip() == null || !ft.getTrip().getId().equals(trip.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expense transaction does not belong to this trip");
        }

        if (ft.getOwner() != null && !ft.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Record belongs to another fleet owner");
        }

        financeTransactionRepository.delete(ft);
        log.info("Audit: Owner '{}' deleted trip expense ID={} for Trip ID={}", ownerUsername, expenseId, tripId);
    }

    private boolean isMaintenanceCategory(String category) {
        if (category == null) return false;
        String upper = category.toUpperCase().trim();
        return upper.contains("MAINTENANCE") ||
                upper.contains("REPAIR") ||
                upper.contains("SERVICE") ||
                upper.contains("OIL") ||
                upper.contains("TYRE") ||
                upper.contains("TIRE") ||
                upper.contains("PUNCTURE") ||
                upper.contains("BRAKE") ||
                upper.contains("BATTERY") ||
                upper.contains("SPARE") ||
                upper.contains("CLUTCH");
    }

    private String normalizeFinanceCategory(String category) {
        if (category == null) return "OTHER";
        String upper = category.toUpperCase().trim();
        if (upper.contains("DIESEL") || upper.contains("FUEL")) return "DIESEL";
        if (upper.contains("TOLL")) return "TOLL";
        if (upper.contains("DRIVER") || upper.contains("SALARY") || upper.contains("BATA") || upper.contains("ALLOWANCE")) return "DRIVER_SALARY";
        if (upper.contains("ADVANCE")) return "ADVANCE";
        if (upper.contains("FREIGHT")) return "FREIGHT";
        return "OTHER";
    }

    private TripDto mapToTripDto(Trip trip) {
        return new TripDto(
                trip.getId(),
                trip.getTripReference(),
                trip.getDriver() != null ? trip.getDriver().getId() : null,
                trip.getDriver() != null ? trip.getDriver().getFullName() : "Unassigned",
                trip.getLorry() != null ? trip.getLorry().getId() : null,
                trip.getLorry() != null ? trip.getLorry().getRegistrationNumber() : "Unknown",
                trip.getLorry() != null ? trip.getLorry().getMakeModel() : null,
                trip.getOriginLocation(),
                trip.getDestinationLocation(),
                trip.getTripStatus() != null ? trip.getTripStatus().name() : "PLANNED",
                trip.getTripDate(),
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getDistanceKm() != null ? trip.getDistanceKm() : BigDecimal.ZERO,
                trip.getDeliveryNotes(),
                trip.getCreatedAt(),
                trip.getPlannedDeliveryTime(),
                trip.getLoadType(),
                trip.getTripAmount() != null ? trip.getTripAmount() : BigDecimal.ZERO
        );
    }
}
