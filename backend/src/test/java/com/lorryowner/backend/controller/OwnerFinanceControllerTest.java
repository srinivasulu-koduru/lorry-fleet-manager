package com.lorryowner.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorryowner.backend.dto.finance.CreateFinanceTransactionRequest;
import com.lorryowner.backend.entity.*;
import com.lorryowner.backend.repository.FinanceTransactionRepository;
import com.lorryowner.backend.repository.LorryRepository;
import com.lorryowner.backend.repository.MaintenanceRecordRepository;
import com.lorryowner.backend.repository.UserRepository;
import com.lorryowner.backend.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OwnerFinanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LorryRepository lorryRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private FinanceTransactionRepository financeTransactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User owner1;
    private User owner2;
    private User driver;
    private Lorry lorry1;
    private Lorry lorry2;
    private String owner1Token;
    private String owner2Token;
    private String driverToken;

    @BeforeEach
    void setUp() {
        tearDown();

        owner1 = new User("fin_owner_1", passwordEncoder.encode("Pass123!"), Role.OWNER, "Finance Owner One", "9876540001", null);
        owner1 = userRepository.save(owner1);

        owner2 = new User("fin_owner_2", passwordEncoder.encode("Pass123!"), Role.OWNER, "Finance Owner Two", "9876540002", null);
        owner2 = userRepository.save(owner2);

        driver = new User("fin_driver_1", passwordEncoder.encode("Pass123!"), Role.DRIVER, "Finance Driver", "9876540003", "DL-F-001");
        driver.setOwner(owner1);
        driver = userRepository.save(driver);

        lorry1 = new Lorry("AP 02 FIN 1001", "Tata Prima 5530", BigDecimal.valueOf(35.0), OperationalStatus.AVAILABLE);
        lorry1.setOwner(owner1);
        lorry1 = lorryRepository.save(lorry1);

        lorry2 = new Lorry("AP 02 FIN 2002", "Ashok Leyland 4220", BigDecimal.valueOf(40.0), OperationalStatus.AVAILABLE);
        lorry2.setOwner(owner2);
        lorry2 = lorryRepository.save(lorry2);

        owner1Token = "Bearer " + jwtService.generateToken(owner1);
        owner2Token = "Bearer " + jwtService.generateToken(owner2);
        driverToken = "Bearer " + jwtService.generateToken(driver);
    }

    @AfterEach
    void tearDown() {
        financeTransactionRepository.deleteAll();
        maintenanceRecordRepository.deleteAll();
        lorryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("T1: Unauthenticated request to /api/owner/finance/summary returns 401 Unauthorized")
    void getFinanceSummary_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/owner/finance/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("T2: Driver role requesting /api/owner/finance/summary returns 403 Forbidden")
    void getFinanceSummary_driverRole_returns403() throws Exception {
        mockMvc.perform(get("/api/owner/finance/summary")
                        .header(HttpHeaders.AUTHORIZATION, driverToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("T3: Empty finance summary returns all 12 months with zeroes and 0.0% profit margin")
    void getFinanceSummary_emptyData_returnsAllZeroesSafely() throws Exception {
        mockMvc.perform(get("/api/owner/finance/summary?year=2026")
                        .header(HttpHeaders.AUTHORIZATION, owner1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpenses").value(0))
                .andExpect(jsonPath("$.netProfit").value(0))
                .andExpect(jsonPath("$.profitMargin").value(0.0))
                .andExpect(jsonPath("$.monthlySummaries", hasSize(12)))
                .andExpect(jsonPath("$.monthlySummaries[0].monthName").value("Jan"))
                .andExpect(jsonPath("$.monthlySummaries[11].monthName").value("Dec"))
                .andExpect(jsonPath("$.quickInsights.profitMargin").value(0.0));
    }

    @Test
    @DisplayName("T4: Income and Expense aggregation correctly calculates profit and profit margin")
    void getFinanceSummary_withTransactions_calculatesAccurately() throws Exception {
        LocalDate currentMonthDate = LocalDate.now().withDayOfMonth(10);

        // 1. Received Income: 1,00,000
        FinanceTransaction income1 = new FinanceTransaction(
                owner1, lorry1, null, "INCOME", "FREIGHT", "Steel freight delivery",
                BigDecimal.valueOf(100000.0), currentMonthDate, "RECEIVED", "BANK_TRANSFER", "INV-1001"
        );
        financeTransactionRepository.save(income1);

        // 2. Pending Income: 50,000 (Should NOT be counted in actual received income!)
        FinanceTransaction pendingIncome = new FinanceTransaction(
                owner1, lorry1, null, "INCOME", "FREIGHT", "Unpaid freight invoice",
                BigDecimal.valueOf(50000.0), currentMonthDate, "PENDING", "BANK_TRANSFER", "INV-1002"
        );
        financeTransactionRepository.save(pendingIncome);

        // 3. Diesel Expense: 30,000
        FinanceTransaction dieselExp = new FinanceTransaction(
                owner1, lorry1, null, "EXPENSE", "DIESEL", "IOCL Bulk diesel refuel",
                BigDecimal.valueOf(30000.0), currentMonthDate, "PAID", "FUEL_CARD", "REC-501"
        );
        financeTransactionRepository.save(dieselExp);

        // 4. Maintenance Expense from maintenance_records: 20,000
        MaintenanceRecord maintExp = new MaintenanceRecord(
                lorry1, owner1, currentMonthDate, "Engine Overhaul", "Full maintenance",
                BigDecimal.valueOf(20000.0), "Tata Authorized Center", 65000, currentMonthDate.plusMonths(3)
        );
        maintenanceRecordRepository.save(maintExp);

        // Expected:
        // Income = 100000.00
        // Expense = 30000.00 (Diesel) + 20000.00 (Maintenance) = 50000.00
        // Profit = 100000 - 50000 = 50000.00
        // Profit Margin = (50000 / 100000) * 100 = 50.0%

        mockMvc.perform(get("/api/owner/finance/summary?year=" + currentMonthDate.getYear())
                        .header(HttpHeaders.AUTHORIZATION, owner1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(100000.0))
                .andExpect(jsonPath("$.totalExpenses").value(50000.0))
                .andExpect(jsonPath("$.netProfit").value(50000.0))
                .andExpect(jsonPath("$.profitMargin").value(50.0))
                .andExpect(jsonPath("$.currentMonthIncome").value(100000.0))
                .andExpect(jsonPath("$.currentMonthExpense").value(50000.0))
                .andExpect(jsonPath("$.currentMonthProfit").value(50000.0));
    }

    @Test
    @DisplayName("T5: Cross-owner finance isolation strictly prevents data leakage")
    void getFinanceSummary_crossOwnerIsolation_owner2CannotSeeOwner1Data() throws Exception {
        LocalDate date = LocalDate.now().withDayOfMonth(5);

        // Owner 1 income: 2,00,000
        FinanceTransaction incomeOwner1 = new FinanceTransaction(
                owner1, lorry1, null, "INCOME", "FREIGHT", "Owner 1 trip",
                BigDecimal.valueOf(200000.0), date, "RECEIVED", "BANK_TRANSFER", "INV-1"
        );
        financeTransactionRepository.save(incomeOwner1);

        // Owner 2 queries summary -> should see 0
        mockMvc.perform(get("/api/owner/finance/summary?year=" + date.getYear())
                        .header(HttpHeaders.AUTHORIZATION, owner2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpenses").value(0))
                .andExpect(jsonPath("$.netProfit").value(0));
    }

    @Test
    @DisplayName("T6: Monthly finance details returns separated transactions and category breakdowns")
    void getMonthlyFinanceDetails_returnsSeparatedTransactionsAndBreakdowns() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 15);

        FinanceTransaction freight = new FinanceTransaction(
                owner1, lorry1, null, "INCOME", "FREIGHT", "Cement delivery",
                BigDecimal.valueOf(125000.0), date, "RECEIVED", "BANK_TRANSFER", "INV-OCT-1"
        );
        financeTransactionRepository.save(freight);

        FinanceTransaction diesel = new FinanceTransaction(
                owner1, lorry1, null, "EXPENSE", "DIESEL", "Highway diesel refuel",
                BigDecimal.valueOf(45000.0), date, "PAID", "CASH", "REC-OCT-2"
        );
        financeTransactionRepository.save(diesel);

        MaintenanceRecord maint = new MaintenanceRecord(
                lorry1, owner1, date, "Brake Pad Replacement", "New pads installed",
                BigDecimal.valueOf(15000.0), "AutoCare Hub", 54000, date.plusMonths(2)
        );
        maintenanceRecordRepository.save(maint);

        mockMvc.perform(get("/api/owner/finance/monthly?year=2026&month=10")
                        .header(HttpHeaders.AUTHORIZATION, owner1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(10))
                .andExpect(jsonPath("$.monthName").value("October"))
                .andExpect(jsonPath("$.totalIncome").value(125000.0))
                .andExpect(jsonPath("$.totalExpenses").value(60000.0)) // 45k diesel + 15k maint
                .andExpect(jsonPath("$.netProfit").value(65000.0))
                .andExpect(jsonPath("$.incomeTransactions", hasSize(1)))
                .andExpect(jsonPath("$.expenseTransactions", hasSize(2)))
                .andExpect(jsonPath("$.expenseCategoryBreakdown.DIESEL").value(45000.0))
                .andExpect(jsonPath("$.expenseCategoryBreakdown.MAINTENANCE").value(15000.0));
    }

    @Test
    @DisplayName("T7: Creating transaction with invalid or negative amount is rejected with 400 Bad Request")
    void createTransaction_invalidAmount_returns400() throws Exception {
        CreateFinanceTransactionRequest request = new CreateFinanceTransactionRequest(
                "EXPENSE", "DIESEL", "Negative amount test",
                BigDecimal.valueOf(-500.0), LocalDate.now(), "PAID", lorry1.getId(), null, "CASH", null
        );

        mockMvc.perform(post("/api/owner/finance/transactions")
                        .header(HttpHeaders.AUTHORIZATION, owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("T8: Creating transaction referencing another owner's lorry is rejected with 403 Forbidden")
    void createTransaction_anotherOwnersLorry_returns403() throws Exception {
        CreateFinanceTransactionRequest request = new CreateFinanceTransactionRequest(
                "EXPENSE", "DIESEL", "Cross owner lorry test",
                BigDecimal.valueOf(5000.0), LocalDate.now(), "PAID", lorry2.getId(), null, "CASH", null
        );

        mockMvc.perform(post("/api/owner/finance/transactions")
                        .header(HttpHeaders.AUTHORIZATION, owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("T9: Valid finance transaction creation succeeds with 201 Created")
    void createTransaction_validRequest_returns21Created() throws Exception {
        CreateFinanceTransactionRequest request = new CreateFinanceTransactionRequest(
                "INCOME", "FREIGHT", "Industrial goods delivery freight payment",
                BigDecimal.valueOf(85000.0), LocalDate.of(2026, 11, 1), "RECEIVED", lorry1.getId(), null, "UPI", "UPI-REF-9988"
        );

        mockMvc.perform(post("/api/owner/finance/transactions")
                        .header(HttpHeaders.AUTHORIZATION, owner1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.category").value("FREIGHT"))
                .andExpect(jsonPath("$.amount").value(85000.0))
                .andExpect(jsonPath("$.paymentStatus").value("RECEIVED"))
                .andExpect(jsonPath("$.lorryRegistration").value("AP 02 FIN 1001"));
    }
}
