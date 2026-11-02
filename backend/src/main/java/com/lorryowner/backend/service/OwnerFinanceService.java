package com.lorryowner.backend.service;

import com.lorryowner.backend.dto.finance.*;
import com.lorryowner.backend.entity.*;
import com.lorryowner.backend.repository.FinanceTransactionRepository;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.MaintenanceRecordRepository;
import com.lorryowner.backend.repository.TripRepository;
import com.lorryowner.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class OwnerFinanceService {

    private final FinanceTransactionRepository financeTransactionRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final UserRepository userRepository;
    private final LorryRepository lorryRepository;
    private final TripRepository tripRepository;

    public OwnerFinanceService(
            FinanceTransactionRepository financeTransactionRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            UserRepository userRepository,
            LorryRepository lorryRepository,
            TripRepository tripRepository
    ) {
        this.financeTransactionRepository = financeTransactionRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.userRepository = userRepository;
        this.lorryRepository = lorryRepository;
        this.tripRepository = tripRepository;
    }

    /**
     * Resolves the authenticated user and strictly enforces the OWNER role.
     */
    public User resolveOwner(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (user.getRole() != Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Owner role required");
        }
        return user;
    }

    /**
     * Returns the yearly financial summary for the specified year (defaults to current year).
     * Includes all 12 month summaries, current month metrics, and quick insights.
     */
    public FinanceYearlySummaryResponse getYearlyFinanceSummary(String username, Integer requestedYear) {
        User owner = resolveOwner(username);
        int currentYear = LocalDate.now().getYear();
        int targetYear = requestedYear != null ? requestedYear : currentYear;

        // Retrieve available distinct years from both finance transactions and maintenance records
        Set<Integer> yearsSet = new TreeSet<>(Collections.reverseOrder());
        yearsSet.add(currentYear);
        yearsSet.addAll(financeTransactionRepository.findDistinctYearsByOwner(owner));
        yearsSet.addAll(maintenanceRecordRepository.findDistinctYearsByOwner(owner));
        List<Integer> availableYears = new ArrayList<>(yearsSet);

        LocalDate startDate = LocalDate.of(targetYear, 1, 1);
        LocalDate endDate = LocalDate.of(targetYear, 12, 31);

        // 1. Grouped finance transactions for the 12 months (Single SQL query)
        List<Object[]> ftRows = financeTransactionRepository.sumByOwnerGroupedByMonthAndType(owner, startDate, endDate);
        Map<Integer, BigDecimal> monthlyIncomeMap = new HashMap<>();
        Map<Integer, BigDecimal> monthlyOtherExpenseMap = new HashMap<>();

        for (Object[] row : ftRows) {
            Integer m = ((Number) row[0]).intValue();
            String type = (String) row[1];
            BigDecimal amt = (BigDecimal) row[2];
            if ("INCOME".equalsIgnoreCase(type)) {
                monthlyIncomeMap.put(m, amt);
            } else if ("EXPENSE".equalsIgnoreCase(type)) {
                monthlyOtherExpenseMap.put(m, amt);
            }
        }

        // 2. Grouped maintenance expenses for the 12 months (Single SQL query)
        List<Object[]> mrRows = maintenanceRecordRepository.sumCostByOwnerGroupedByMonth(owner, startDate, endDate);
        Map<Integer, BigDecimal> monthlyMaintMap = new HashMap<>();
        for (Object[] row : mrRows) {
            Integer m = ((Number) row[0]).intValue();
            BigDecimal cost = (BigDecimal) row[1];
            monthlyMaintMap.put(m, cost);
        }

        // 3. Assemble 12 months
        List<MonthlySummaryDto> monthlySummaries = new ArrayList<>(12);
        BigDecimal totalYearlyIncome = BigDecimal.ZERO;
        BigDecimal totalYearlyExpenses = BigDecimal.ZERO;

        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        int bestMonthIdx = 1;
        BigDecimal bestMonthProfit = null;
        int highestExpenseMonthIdx = 1;
        BigDecimal highestExpenseMonthAmount = BigDecimal.ZERO;

        LocalDate now = LocalDate.now();
        int currentMonthNum = now.getMonthValue();
        BigDecimal ytdIncome = BigDecimal.ZERO;
        BigDecimal ytdExpense = BigDecimal.ZERO;

        for (int m = 1; m <= 12; m++) {
            BigDecimal inc = monthlyIncomeMap.getOrDefault(m, BigDecimal.ZERO);
            BigDecimal maint = monthlyMaintMap.getOrDefault(m, BigDecimal.ZERO);
            BigDecimal otherExp = monthlyOtherExpenseMap.getOrDefault(m, BigDecimal.ZERO);
            BigDecimal exp = maint.add(otherExp);
            BigDecimal profit = inc.subtract(exp);

            totalYearlyIncome = totalYearlyIncome.add(inc);
            totalYearlyExpenses = totalYearlyExpenses.add(exp);

            if (bestMonthProfit == null || profit.compareTo(bestMonthProfit) > 0) {
                bestMonthProfit = profit;
                bestMonthIdx = m;
            }

            if (exp.compareTo(highestExpenseMonthAmount) > 0) {
                highestExpenseMonthAmount = exp;
                highestExpenseMonthIdx = m;
            }

            // Calculate YTD if in current year
            if (targetYear < currentYear || (targetYear == currentYear && m <= currentMonthNum)) {
                ytdIncome = ytdIncome.add(inc);
                ytdExpense = ytdExpense.add(exp);
            }

            monthlySummaries.add(new MonthlySummaryDto(
                    m,
                    monthNames[m - 1],
                    inc,
                    exp,
                    profit
            ));
        }

        BigDecimal netYearlyProfit = totalYearlyIncome.subtract(totalYearlyExpenses);
        double profitMargin = calculateProfitMargin(totalYearlyIncome, netYearlyProfit);

        // Determine current month metrics
        int evalMonthIdx = (targetYear == currentYear) ? currentMonthNum : 12;
        MonthlySummaryDto currMonthDto = monthlySummaries.get(evalMonthIdx - 1);
        String currentMonthName = LocalDate.of(targetYear, evalMonthIdx, 1)
                .getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        BigDecimal ytdProfit = ytdIncome.subtract(ytdExpense);

        FinanceQuickInsightsDto insights = new FinanceQuickInsightsDto(
                profitMargin,
                currMonthDto.profit(),
                ytdIncome,
                ytdExpense,
                ytdProfit,
                monthNames[bestMonthIdx - 1],
                bestMonthProfit != null ? bestMonthProfit : BigDecimal.ZERO,
                monthNames[highestExpenseMonthIdx - 1],
                highestExpenseMonthAmount
        );

        return new FinanceYearlySummaryResponse(
                targetYear,
                availableYears,
                totalYearlyIncome,
                totalYearlyExpenses,
                netYearlyProfit,
                profitMargin,
                currentMonthName,
                currMonthDto.income(),
                currMonthDto.expense(),
                currMonthDto.profit(),
                monthlySummaries,
                insights
        );
    }

    /**
     * Returns detailed financial history, income/expense transaction lists,
     * and category breakdowns for a specific month and year.
     */
    public MonthlyFinanceDetailsResponse getMonthlyFinanceDetails(String username, int year, int month) {
        User owner = resolveOwner(username);
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must be between 1 and 12");
        }

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        String monthName = startDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        // 1. Fetch Finance Transactions for this month
        List<FinanceTransaction> ftList = financeTransactionRepository
                .findByOwnerAndTransactionDateBetweenOrderByTransactionDateDesc(owner, startDate, endDate);

        // 2. Fetch Maintenance Records for this month (authoritative expense source)
        List<MaintenanceRecord> mrList = maintenanceRecordRepository
                .findByOwnerAndMaintenanceDateBetweenOrderByMaintenanceDateDesc(owner, startDate, endDate);

        List<FinanceTransactionDto> incomeTransactions = new ArrayList<>();
        List<FinanceTransactionDto> expenseTransactions = new ArrayList<>();

        Map<String, BigDecimal> incomeCategoryBreakdown = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseCategoryBreakdown = new LinkedHashMap<>();

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        // Process Finance Transactions
        for (FinanceTransaction ft : ftList) {
            FinanceTransactionDto dto = mapToDto(ft);
            if ("INCOME".equalsIgnoreCase(ft.getTransactionType())) {
                incomeTransactions.add(dto);
                if ("RECEIVED".equalsIgnoreCase(ft.getPaymentStatus())) {
                    totalIncome = totalIncome.add(ft.getAmount());
                    incomeCategoryBreakdown.merge(ft.getCategory(), ft.getAmount(), BigDecimal::add);
                }
            } else if ("EXPENSE".equalsIgnoreCase(ft.getTransactionType())) {
                expenseTransactions.add(dto);
                totalExpenses = totalExpenses.add(ft.getAmount());
                expenseCategoryBreakdown.merge(ft.getCategory(), ft.getAmount(), BigDecimal::add);
            }
        }

        // Process Maintenance Records as unified Maintenance Expense items
        for (MaintenanceRecord mr : mrList) {
            FinanceTransactionDto maintDto = new FinanceTransactionDto(
                    mr.getId(),
                    mr.getLorry() != null ? mr.getLorry().getId() : null,
                    mr.getLorry() != null ? mr.getLorry().getRegistrationNumber() : "Fleet Lorry",
                    null,
                    null,
                    "EXPENSE",
                    "MAINTENANCE",
                    mr.getServiceType() + (mr.getDescription() != null && !mr.getDescription().isBlank() ? " (" + mr.getDescription() + ")" : ""),
                    mr.getCost(),
                    mr.getMaintenanceDate(),
                    "PAID",
                    "CASH",
                    mr.getServiceCenter()
            );
            expenseTransactions.add(maintDto);
            totalExpenses = totalExpenses.add(mr.getCost());
            expenseCategoryBreakdown.merge("MAINTENANCE", mr.getCost(), BigDecimal::add);
        }

        // Sort expense transactions chronologically descending
        expenseTransactions.sort((a, b) -> b.transactionDate().compareTo(a.transactionDate()));

        BigDecimal netProfit = totalIncome.subtract(totalExpenses);
        double profitMargin = calculateProfitMargin(totalIncome, netProfit);

        return new MonthlyFinanceDetailsResponse(
                year,
                month,
                monthName,
                totalIncome,
                totalExpenses,
                netProfit,
                profitMargin,
                incomeTransactions,
                expenseTransactions,
                expenseCategoryBreakdown,
                incomeCategoryBreakdown
        );
    }

    /**
     * Returns full yearly analysis with quarterly and category comparisons.
     */
    public YearlyAnalysisResponse getYearlyAnalysis(String username, int year) {
        User owner = resolveOwner(username);
        FinanceYearlySummaryResponse summary = getYearlyFinanceSummary(username, year);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<Object[]> catRows = financeTransactionRepository.sumByOwnerGroupedByCategory(owner, startDate, endDate);
        Map<String, BigDecimal> incomeCategories = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseCategories = new LinkedHashMap<>();

        for (Object[] row : catRows) {
            String cat = (String) row[0];
            String type = (String) row[1];
            BigDecimal sum = (BigDecimal) row[2];

            if ("INCOME".equalsIgnoreCase(type)) {
                incomeCategories.put(cat, sum);
            } else if ("EXPENSE".equalsIgnoreCase(type)) {
                expenseCategories.put(cat, sum);
            }
        }

        // Add maintenance records yearly total into expense categories
        BigDecimal yearlyMaintTotal = maintenanceRecordRepository.sumCostByOwnerAndDateRange(owner, startDate, endDate);
        if (yearlyMaintTotal != null && yearlyMaintTotal.compareTo(BigDecimal.ZERO) > 0) {
            expenseCategories.merge("MAINTENANCE", yearlyMaintTotal, BigDecimal::add);
        }

        return new YearlyAnalysisResponse(
                year,
                summary.totalIncome(),
                summary.totalExpenses(),
                summary.netProfit(),
                summary.profitMargin(),
                summary.monthlySummaries(),
                expenseCategories,
                incomeCategories,
                summary.quickInsights()
        );
    }

    /**
     * Creates an authoritative finance transaction (Income or Expense) for the authenticated owner.
     */
    @Transactional
    public FinanceTransactionDto createTransaction(String username, CreateFinanceTransactionRequest request) {
        User owner = resolveOwner(username);

        String type = request.transactionType().trim().toUpperCase();
        if (!"INCOME".equals(type) && !"EXPENSE".equals(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction type must be INCOME or EXPENSE");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be strictly greater than zero");
        }

        if (request.transactionDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction date is required");
        }

        Lorry lorry = null;
        if (request.lorryId() != null) {
            lorry = lorryRepository.findById(request.lorryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lorry not found"));
            if (lorry.getOwner() == null || !lorry.getOwner().getId().equals(owner.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lorry does not belong to authenticated owner");
            }
        }

        Trip trip = null;
        if (request.tripId() != null) {
            trip = tripRepository.findById(request.tripId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
            if (trip.getOwner() == null || !trip.getOwner().getId().equals(owner.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Trip does not belong to authenticated owner");
            }
        }

        String paymentStatus = request.paymentStatus() != null && !request.paymentStatus().isBlank()
                ? request.paymentStatus().trim().toUpperCase()
                : ("INCOME".equals(type) ? "RECEIVED" : "PAID");

        FinanceTransaction entity = new FinanceTransaction(
                owner,
                lorry,
                trip,
                type,
                request.category().trim().toUpperCase(),
                request.description().trim(),
                request.amount(),
                request.transactionDate(),
                paymentStatus,
                request.paymentMode(),
                request.referenceNumber()
        );

        FinanceTransaction saved = financeTransactionRepository.save(entity);
        return mapToDto(saved);
    }

    private double calculateProfitMargin(BigDecimal income, BigDecimal profit) {
        if (income == null || income.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return profit.divide(income, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public FinanceTransactionDto mapToDto(FinanceTransaction ft) {
        return new FinanceTransactionDto(
                ft.getId(),
                ft.getLorry() != null ? ft.getLorry().getId() : null,
                ft.getLorry() != null ? ft.getLorry().getRegistrationNumber() : null,
                ft.getTrip() != null ? ft.getTrip().getId() : null,
                ft.getTrip() != null ? ft.getTrip().getTripReference() : null,
                ft.getTransactionType(),
                ft.getCategory(),
                ft.getDescription(),
                ft.getAmount(),
                ft.getTransactionDate(),
                ft.getPaymentStatus(),
                ft.getPaymentMode(),
                ft.getReferenceNumber()
        );
    }
}
