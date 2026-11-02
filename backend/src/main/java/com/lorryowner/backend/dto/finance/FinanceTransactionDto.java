package com.lorryowner.backend.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinanceTransactionDto(
        Long id,
        Long lorryId,
        String lorryRegistration,
        Long tripId,
        String tripReference,
        String transactionType, // "INCOME" or "EXPENSE"
        String category,        // "FREIGHT", "ADVANCE", "DIESEL", "TOLL", "DRIVER_SALARY", "MAINTENANCE", "OTHER"
        String description,
        BigDecimal amount,
        LocalDate transactionDate,
        String paymentStatus,   // "RECEIVED", "PENDING"
        String paymentMode,
        String referenceNumber
) {}
