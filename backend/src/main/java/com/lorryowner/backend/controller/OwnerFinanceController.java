package com.lorryowner.backend.controller;

import com.lorryowner.backend.dto.finance.*;
import com.lorryowner.backend.service.OwnerFinanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/owner/finance")
@PreAuthorize("hasRole('OWNER')")
public class OwnerFinanceController {

    private final OwnerFinanceService ownerFinanceService;

    public OwnerFinanceController(OwnerFinanceService ownerFinanceService) {
        this.ownerFinanceService = ownerFinanceService;
    }

    /**
     * Retrieves yearly finance summary including all 12 monthly totals,
     * current month financial snapshot, and quick insights for the specified year.
     */
    @GetMapping("/summary")
    public ResponseEntity<FinanceYearlySummaryResponse> getYearlyFinanceSummary(
            @RequestParam(name = "year", required = false) Integer year,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        FinanceYearlySummaryResponse summary = ownerFinanceService.getYearlyFinanceSummary(username, year);
        return ResponseEntity.ok(summary);
    }

    /**
     * Retrieves detailed financial breakdown for a specific calendar month,
     * including income transactions, expense transactions (with maintenance records), and category breakdown.
     */
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyFinanceDetailsResponse> getMonthlyFinanceDetails(
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        MonthlyFinanceDetailsResponse details = ownerFinanceService.getMonthlyFinanceDetails(username, year, month);
        return ResponseEntity.ok(details);
    }

    /**
     * Retrieves full annual financial analysis including monthly comparisons and category breakdown.
     */
    @GetMapping("/yearly-analysis")
    public ResponseEntity<YearlyAnalysisResponse> getYearlyAnalysis(
            @RequestParam(name = "year") int year,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        YearlyAnalysisResponse analysis = ownerFinanceService.getYearlyAnalysis(username, year);
        return ResponseEntity.ok(analysis);
    }

    /**
     * Creates an authoritative financial transaction (Income or Expense) for the authenticated owner.
     */
    @PostMapping("/transactions")
    public ResponseEntity<FinanceTransactionDto> createTransaction(
            @Valid @RequestBody CreateFinanceTransactionRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        FinanceTransactionDto saved = ownerFinanceService.createTransaction(username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
