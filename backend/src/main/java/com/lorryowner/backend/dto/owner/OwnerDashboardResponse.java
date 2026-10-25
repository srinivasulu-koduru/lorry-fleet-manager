package com.lorryowner.backend.dto.owner;

import java.util.List;

public record OwnerDashboardResponse(
    String ownerName,
    int unreadNotificationsCount,
    FleetOverviewDto fleetOverview,
    FinancialSummaryDto financialSummary,
    List<RecentActivityDto> recentActivities
) {}
