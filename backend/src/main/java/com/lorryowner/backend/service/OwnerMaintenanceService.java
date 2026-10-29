package com.lorryowner.backend.service;

import com.lorryowner.backend.dto.lorry.MaintenanceRecordDto;
import com.lorryowner.backend.dto.maintenance.CreateMaintenanceRecordRequest;
import com.lorryowner.backend.dto.maintenance.MaintenanceSummaryResponse;
import com.lorryowner.backend.dto.maintenance.VehicleMaintenanceDetailsResponse;
import com.lorryowner.backend.dto.maintenance.VehicleMaintenanceSummaryDto;
import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.MaintenanceRecord;
import com.lorryowner.backend.entity.Role;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.MaintenanceRecordRepository;
import com.lorryowner.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OwnerMaintenanceService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final LorryRepository lorryRepository;
    private final UserRepository userRepository;

    public OwnerMaintenanceService(
            MaintenanceRecordRepository maintenanceRecordRepository,
            LorryRepository lorryRepository,
            UserRepository userRepository
    ) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.lorryRepository = lorryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Resolves the authenticated owner user and verifies role.
     */
    public User resolveOwner(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated session");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (user.getRole() != Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access restricted to owners");
        }
        return user;
    }

    /**
     * Retrieves lorry with strict owner data-isolation checks.
     */
    public Lorry getLorryWithOwnershipCheck(Long lorryId, User owner) {
        Lorry lorry = lorryRepository.findById(lorryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lorry not found with id: " + lorryId));

        if (lorry.getOwner() != null && !lorry.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Lorry belongs to another owner");
        }
        return lorry;
    }

    /**
     * Business logic for calculating maintenance status:
     * - If nextServiceDate is in the past: "Overdue"
     * - If nextServiceDate is within the next 30 days: "Due Soon"
     * - Otherwise (or if not scheduled): "Up to Date"
     */
    public String calculateStatus(LocalDate nextServiceDate, LocalDate referenceDate) {
        if (nextServiceDate == null) {
            return "Up to Date";
        }
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();

        if (nextServiceDate.isBefore(ref)) {
            return "Overdue";
        } else if (!nextServiceDate.isAfter(ref.plusDays(30))) {
            return "Due Soon";
        } else {
            return "Up to Date";
        }
    }

    /**
     * Returns the maintenance summary for all lorries owned by the authenticated owner.
     */
    public MaintenanceSummaryResponse getMaintenanceSummary(String username) {
        User owner = resolveOwner(username);
        List<Lorry> ownedLorries = lorryRepository.findByOwnerOrderByRegistrationNumberAsc(owner);
        LocalDate today = LocalDate.now();

        List<VehicleMaintenanceSummaryDto> vehicleSummaries = new ArrayList<>();
        long dueSoonCount = 0;
        long overdueCount = 0;

        for (Lorry lorry : ownedLorries) {
            List<MaintenanceRecord> records = maintenanceRecordRepository.findByLorryOrderByMaintenanceDateDesc(lorry);
            LocalDate lastServiceDate = records.isEmpty() ? null : records.get(0).getMaintenanceDate();

            // Find the most recent nextServiceDate
            LocalDate nextServiceDate = null;
            for (MaintenanceRecord r : records) {
                if (r.getNextServiceDate() != null) {
                    nextServiceDate = r.getNextServiceDate();
                    break;
                }
            }

            BigDecimal totalCost = maintenanceRecordRepository.sumCostByLorry(lorry);
            String status = calculateStatus(nextServiceDate, today);

            if ("Overdue".equals(status)) {
                overdueCount++;
            } else if ("Due Soon".equals(status)) {
                dueSoonCount++;
            }

            vehicleSummaries.add(new VehicleMaintenanceSummaryDto(
                    lorry.getId(),
                    lorry.getRegistrationNumber(),
                    lorry.getMakeModel(),
                    lastServiceDate,
                    nextServiceDate,
                    status,
                    totalCost != null ? totalCost : BigDecimal.ZERO,
                    records.size()
            ));
        }

        BigDecimal totalExpenditure = maintenanceRecordRepository.sumCostByOwner(owner);
        if (totalExpenditure == null) {
            totalExpenditure = BigDecimal.ZERO;
        }

        return new MaintenanceSummaryResponse(
                ownedLorries.size(),
                dueSoonCount,
                overdueCount,
                totalExpenditure,
                vehicleSummaries
        );
    }

    /**
     * Lists all maintenance records belonging to the authenticated owner.
     */
    public List<MaintenanceRecordDto> getAllMaintenanceRecords(String username) {
        User owner = resolveOwner(username);
        List<MaintenanceRecord> records = maintenanceRecordRepository.findByOwnerOrderByMaintenanceDateDesc(owner);

        return records.stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Retrieves vehicle maintenance details and chronological service history for a specific lorry.
     */
    public VehicleMaintenanceDetailsResponse getLorryMaintenanceDetails(Long lorryId, String username) {
        User owner = resolveOwner(username);
        Lorry lorry = getLorryWithOwnershipCheck(lorryId, owner);

        List<MaintenanceRecord> records = maintenanceRecordRepository.findByLorryOrderByMaintenanceDateDesc(lorry);
        BigDecimal totalCost = maintenanceRecordRepository.sumCostByLorry(lorry);
        if (totalCost == null) {
            totalCost = BigDecimal.ZERO;
        }

        LocalDate lastServiceDate = records.isEmpty() ? null : records.get(0).getMaintenanceDate();
        LocalDate nextServiceDate = null;
        for (MaintenanceRecord r : records) {
            if (r.getNextServiceDate() != null) {
                nextServiceDate = r.getNextServiceDate();
                break;
            }
        }

        String status = calculateStatus(nextServiceDate, LocalDate.now());
        List<MaintenanceRecordDto> dtoList = records.stream().map(this::mapToDto).toList();

        return new VehicleMaintenanceDetailsResponse(
                lorry.getId(),
                lorry.getRegistrationNumber(),
                lorry.getMakeModel(),
                lastServiceDate,
                nextServiceDate,
                status,
                totalCost,
                dtoList
        );
    }

    /**
     * Creates a new maintenance record with strict ownership verification and validation rules.
     */
    @Transactional
    public MaintenanceRecordDto createMaintenanceRecord(Long explicitLorryId, CreateMaintenanceRecordRequest request, String username) {
        User owner = resolveOwner(username);
        Long targetLorryId = explicitLorryId != null ? explicitLorryId : request.lorryId();

        if (targetLorryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lorry ID must be provided");
        }

        Lorry lorry = getLorryWithOwnershipCheck(targetLorryId, owner);

        // Validation: service date required
        if (request.maintenanceDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service date is required");
        }

        // Validation: nextServiceDate cannot be before maintenanceDate
        if (request.nextServiceDate() != null && request.nextServiceDate().isBefore(request.maintenanceDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Next service date cannot be earlier than service date");
        }

        // Validation: non-negative cost
        if (request.cost() == null || request.cost().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maintenance cost cannot be negative");
        }

        // Validation: non-negative odometer
        if (request.odometerReadingKm() == null || request.odometerReadingKm() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Odometer reading cannot be negative");
        }

        // Description / notes formatting
        String description = request.description();
        if (request.notes() != null && !request.notes().isBlank()) {
            description = (description != null ? description.trim() + "\n" : "") + "Notes: " + request.notes().trim();
        }

        MaintenanceRecord record = new MaintenanceRecord(
                lorry,
                owner,
                request.maintenanceDate(),
                request.serviceType(),
                description,
                request.cost(),
                request.serviceCenter(),
                request.odometerReadingKm(),
                request.nextServiceDate()
        );

        MaintenanceRecord saved = maintenanceRecordRepository.save(record);
        return mapToDto(saved);
    }

    private MaintenanceRecordDto mapToDto(MaintenanceRecord r) {
        return new MaintenanceRecordDto(
                r.getId(),
                r.getLorry().getId(),
                r.getLorry().getRegistrationNumber(),
                r.getMaintenanceDate(),
                r.getServiceType(),
                r.getDescription(),
                r.getCost(),
                r.getServiceCenter(),
                r.getOdometerReadingKm(),
                r.getNextServiceDate()
        );
    }
}
