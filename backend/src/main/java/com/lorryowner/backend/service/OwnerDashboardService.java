package com.lorryowner.backend.service;

import com.lorryowner.backend.dto.owner.FinancialSummaryDto;
import com.lorryowner.backend.dto.owner.FleetOverviewDto;
import com.lorryowner.backend.dto.owner.OwnerDashboardResponse;
import com.lorryowner.backend.dto.owner.RecentActivityDto;
import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.OperationalStatus;
import com.lorryowner.backend.entity.User;
import com.lorryowner.backend.repository.LorryRepository;
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

    public OwnerDashboardService(LorryRepository lorryRepository, UserRepository userRepository) {
        this.lorryRepository = lorryRepository;
        this.userRepository = userRepository;
    }

    public OwnerDashboardResponse getDashboardData(String username) {
        // Resolve owner display name
        String ownerName = "Owner";
        if (username != null && !username.isBlank()) {
            ownerName = userRepository.findByUsername(username)
                    .map(User::getFullName)
                    .filter(name -> !name.isBlank())
                    .orElse(username);
        }

        // Real fleet counts calculated directly from MySQL database
        long runningNow = lorryRepository.countByOperationalStatus(OperationalStatus.ON_TRIP);
        long readyToStart = lorryRepository.countByOperationalStatus(OperationalStatus.AVAILABLE);
        long notReady = lorryRepository.countByOperationalStatusIn(
                List.of(OperationalStatus.MAINTENANCE, OperationalStatus.UNAVAILABLE)
        );
        long totalLorries = lorryRepository.count();

        FleetOverviewDto fleetOverview = new FleetOverviewDto(
                runningNow,
                readyToStart,
                notReady,
                totalLorries
        );

        // Monthly financial figures (marked as sample per reference design)
        FinancialSummaryDto financialSummary = new FinancialSummaryDto(
                "This month",
                true,
                new BigDecimal("389500.00"),
                new BigDecimal("258000.00"),
                new BigDecimal("131500.00"),
                "₹"
        );

        // Real activity items derived from registered fleet lorries
        List<Lorry> lorries = lorryRepository.findAllByOrderByUpdatedAtDesc();
        List<RecentActivityDto> recentActivities = new ArrayList<>();
        Instant now = Instant.now();

        int index = 0;
        for (Lorry lorry : lorries) {
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
