package com.lorryowner.backend.dto.trip;

import com.lorryowner.backend.dto.finance.FinanceTransactionDto;
import com.lorryowner.backend.dto.lorry.MaintenanceRecordDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TripDetailResponseDto(
        Long id,
        String tripReference,
        Long lorryId,
        String lorryRegistration,
        String lorryMakeModel,
        Long driverId,
        String driverName,
        String driverPhone,
        String origin,
        String destination,
        LocalDate plannedDepartureDate,
        LocalTime plannedDepartureTime,
        Instant startTime,
        LocalDate plannedDeliveryDate,
        LocalTime plannedDeliveryTime,
        Instant endTime,
        String loadType,
        String remarks,
        String status,
        BigDecimal distanceKm,
        Instant createdAt,
        // Financial Breakdown (server-calculated using reliable decimal types)
        BigDecimal tripRevenue,
        BigDecimal maintenanceExpenses,
        BigDecimal fuelExpenses,
        BigDecimal driverExpenses,
        BigDecimal otherExpenses,
        BigDecimal totalExpenses,
        BigDecimal netProfit,
        // Itemized records
        List<MaintenanceRecordDto> maintenanceRecords,
        List<FinanceTransactionDto> otherExpensesList
) {}
