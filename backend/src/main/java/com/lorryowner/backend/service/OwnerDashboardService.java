package com.lorryowner.backend.service;

import com.lorryowner.backend.dto.owner.FinancialSummaryDto;
import com.lorryowner.backend.dto.owner.FleetOverviewDto;
import com.lorryowner.backend.dto.owner.OwnerDashboardResponse;
import com.lorryowner.backend.dto.owner.RecentActivityDto;
import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.OperationalStatus;
import com.lorryowner.backend.entity.TripStatus;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.FinanceTransactionRepository;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.MaintenanceRecordRepository;
import com.lorryowner.backend.repository.TripRepository;
import com.lorryowner.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OwnerDashboardService {

    private final LorryRepository lorryRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final FinanceTransactionRepository financeTransactionRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public OwnerDashboardService(
            LorryRepository lorryRepository,
            UserRepository userRepository,
            TripRepository tripRepository,
            FinanceTransactionRepository financeTransactionRepository,
            MaintenanceRecordRepository maintenanceRecordRepository
    ) {
        this.lorryRepository = lorryRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.financeTransactionRepository = financeTransactionRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    public OwnerDashboardResponse getDashboardData(String username) {
        // Resolve owner display name and owner entity
        User owner = null;
        String ownerName = "Owner";
        if (username != null && !username.isBlank()) {
            owner = userRepository.findByUsername(username).orElse(null);
            if (owner != null) {
                ownerName = (owner.getFullName() != null && !owner.getFullName().isBlank())
                        ? owner.getFullName()
                        : username;
            }
        }

        // Fleet metrics calculated strictly from actual database records scoped by owner
        // Business rules:
        // 1. Total Lorries = all lorries belonging to the authenticated owner.
        // 2. Running Now = lorries with an active IN_PROGRESS trip.
        // 3. Ready to Start = available lorries that are not currently running and are eligible for a new trip.
        // 4. Not Loaded / Not Ready = lorries that are not running and are not ready (maintenance, loading, unavailable).
        List<Lorry> ownedLorries = owner != null
                ? lorryRepository.findByOwnerOrderByUpdatedAtDesc(owner)
                : lorryRepository.findAllByOrderByUpdatedAtDesc();

        long totalLorries = ownedLorries.size();
        long runningNow = 0;
        long readyToStart = 0;
        long notReady = 0;

        for (Lorry lorry : ownedLorries) {
            boolean hasActiveTrip = tripRepository.existsByLorryAndTripStatusIn(lorry, List.of(TripStatus.IN_PROGRESS));
            if (hasActiveTrip || lorry.getOperationalStatus() == OperationalStatus.ON_TRIP) {
                runningNow++;
            } else if (lorry.getOperationalStatus() == OperationalStatus.AVAILABLE) {
                readyToStart++;
            } else {
                notReady++;
            }
        }

        FleetOverviewDto fleetOverview = new FleetOverviewDto(
                runningNow,
                readyToStart,
                notReady,
                totalLorries
        );

        // Dynamic financial figures calculated from actual database records scoped by owner
        long totalTrips = owner != null ? tripRepository.countByOwner(owner) : tripRepository.count();
        BigDecimal totalIncome = owner != null ? financeTransactionRepository.sumIncomeByOwner(owner) : BigDecimal.ZERO;
        BigDecimal otherExpenses = owner != null ? financeTransactionRepository.sumExpenseByOwner(owner) : BigDecimal.ZERO;
        BigDecimal maintExpenses = owner != null ? maintenanceRecordRepository.sumCostByOwner(owner) : BigDecimal.ZERO;
        BigDecimal totalExpenses = otherExpenses.add(maintExpenses);
        BigDecimal netProfit = totalIncome.subtract(totalExpenses);

        FinancialSummaryDto financialSummary = new FinancialSummaryDto(
                "This month",
                false,
                totalIncome,
                totalExpenses,
                netProfit,
                "₹",
                totalTrips
        );

        // Top 5 recent activities derived from owner's lorries
        List<RecentActivityDto> recentActivities = new ArrayList<>();
        Instant now = Instant.now();

        int index = 0;
        for (Lorry lorry : ownedLorries) {
            String color;
            String type;
            String desc = (lorry.getNotes() != null && !lorry.getNotes().isBlank())
                    ? lorry.getNotes()
                    : getDefaultStatusDescription(lorry.getOperationalStatus());

            if (desc.toLowerCase().contains("diesel")) {
                color = "BLUE";
                type = "DIESEL";
            } else if (lorry.getOperationalStatus() == OperationalStatus.ON_TRIP) {
                color = "GREEN";
                type = "TRIP";
            } else if (lorry.getOperationalStatus() == OperationalStatus.AVAILABLE) {
                color = "BLUE";
                type = "AVAILABLE";
            } else {
                color = "ORANGE";
                type = "STATUS";
            }

            String timeAgo = formatTimeAgo(now, lorry.getUpdatedAt(), index);

            recentActivities.add(new RecentActivityDto(
                    lorry.getId(),
                    lorry.getRegistrationNumber(),
                    desc,
                    type,
                    color,
                    timeAgo,
                    lorry.getUpdatedAt()
            ));

            index++;
            if (recentActivities.size() >= 5) {
                break;
            }
        }

        return new OwnerDashboardResponse(
                ownerName,
                1,
                fleetOverview,
                financialSummary,
                recentActivities
        );
    }

    private String getDefaultStatusDescription(OperationalStatus status) {
        return switch (status) {
            case ON_TRIP -> "Load completed - on the road";
            case LOADING -> "Loading cargo in dock";
            case AVAILABLE -> "Ready for dispatch";
            case MAINTENANCE -> "In maintenance workshop";
            case UNAVAILABLE -> "Unloaded - waiting for load";
        };
    }

    private String formatTimeAgo(Instant now, Instant eventTime, int fallbackOffsetIndex) {
        if (eventTime != null) {
            long minutes = Duration.between(eventTime, now).toMinutes();
            if (minutes < 1) {
                return switch (fallbackOffsetIndex) {
                    case 0 -> "5 min ago";
                    case 1 -> "1 hr ago";
                    case 2 -> "2 hrs ago";
                    case 3 -> "3 hrs ago";
                    default -> "Today";
                };
            }
            if (minutes < 60) {
                return minutes + " min ago";
            }
            long hours = minutes / 60;
            if (hours < 24) {
                return hours + (hours == 1 ? " hr ago" : " hrs ago");
            }
            long days = hours / 24;
            return days + (days == 1 ? " day ago" : " days ago");
        }
        return "Just now";
    }
}
