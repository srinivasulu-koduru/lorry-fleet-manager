package com.lorryowner.backend.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateFinanceTransactionRequest(
        @NotNull(message = "Transaction type is required (INCOME or EXPENSE)")
        String transactionType,

        @NotBlank(message = "Category is required")
        String category,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be strictly greater than zero")
        BigDecimal amount,

        @NotNull(message = "Transaction date is required")
        LocalDate transactionDate,

        String paymentStatus,
        Long lorryId,
        Long tripId,
        String paymentMode,
        String referenceNumber
) {}
