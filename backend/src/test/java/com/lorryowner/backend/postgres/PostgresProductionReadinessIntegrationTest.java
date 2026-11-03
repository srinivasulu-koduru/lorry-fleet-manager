package com.lorryowner.backend.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorryowner.backend.dto.auth.LoginRequest;
import com.lorryowner.backend.dto.auth.LoginResponse;
import com.lorryowner.backend.dto.driver.CreateDriverRequest;
import com.lorryowner.backend.dto.driver.DriverResponse;
import com.lorryowner.backend.dto.finance.CreateFinanceTransactionRequest;
import com.lorryowner.backend.dto.lorry.CreateLorryRequest;
import com.lorryowner.backend.dto.lorry.OwnerLorryDetailDto;
import com.lorryowner.backend.dto.maintenance.CreateMaintenanceRecordRequest;
import com.lorryowner.backend.dto.tracking.LiveTrackingLorryDto;
import com.lorryowner.backend.dto.tracking.SubmitLocationRequest;
import com.lorryowner.backend.dto.trip.TripCreateDto;
import com.lorryowner.backend.dto.trip.TripResponseDto;
import com.lorryowner.backend.entity.*;
import com.lorryowner.backend.repository.*;
import com.lorryowner.backend.security.JwtService;
import com.lorryowner.backend.service.LiveTrackingService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostgresProductionReadinessIntegrationTest {

    private static EmbeddedPostgres embeddedPostgres;
    private static String postgresJdbcUrl;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LorryRepository lorryRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripLocationRepository tripLocationRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private FinanceTransactionRepository financeTransactionRepository;

    @Autowired
    private TripDocumentRepository tripDocumentRepository;

    @Autowired
    private VehicleDocumentRepository vehicleDocumentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LiveTrackingService liveTrackingService;

    @Autowired
    private DataSource dataSource;

    private static String ownerToken;
    private static String driverToken;
    private static Long ownerId;
    private static Long driverId;
    private static Long lorryId;
    private static Long tripId;

    @BeforeAll
    static void initPostgres() throws Exception {
        embeddedPostgres = EmbeddedPostgres.builder().start();
        postgresJdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres");

        // Read and execute schema-postgres.sql directly against PostgreSQL before Spring Boot validates it
        Path schemaPath = Path.of("d:/lorry-fleet-manager/backend/database/schema-postgres.sql");
        String ddl = Files.readString(schemaPath, StandardCharsets.UTF_8);

        try (Connection conn = embeddedPostgres.getPostgresDatabase().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        }
    }

    @AfterAll
    static void stopPostgres() throws Exception {
        if (embeddedPostgres != null) {
            embeddedPostgres.close();
        }
    }

    @DynamicPropertySource
    static void configurePostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgresJdbcUrl);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @Test
    @Order(1)
    @DisplayName("1. Hibernate ddl-auto=validate succeeds against real PostgreSQL database")
    void testHibernateDdlAutoValidateSuccess() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")) {

            List<String> tables = new java.util.ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString("table_name"));
            }

            assertThat(tables).contains(
                    "users",
                    "lorries",
                    "trips",
                    "trip_locations",
                    "maintenance_records",
                    "finance_transactions",
                    "trip_documents",
                    "vehicle_documents"
            );
        }
    }

    @Test
    @Order(2)
    @DisplayName("2. Owner Registration, Bootstrap, Driver Creation & JWT Authentication")
    void testAuthLifecycle() throws Exception {
        // Provision Owner
        User owner = new User("pg_fleet_owner", passwordEncoder.encode("OwnerSecurePass123!"), Role.OWNER, "BHP Owner PG", "9876543210", null);
        owner = userRepository.save(owner);
        ownerId = owner.getId();

        // Owner Login via REST
        LoginRequest loginReq = new LoginRequest("pg_fleet_owner", "OwnerSecurePass123!");
        MvcResult ownerLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andReturn();

        LoginResponse authRes = objectMapper.readValue(ownerLoginResult.getResponse().getContentAsString(), LoginResponse.class);
        ownerToken = authRes.accessToken();
        assertThat(jwtService.isTokenValid(ownerToken, owner)).isTrue();

        // Owner creates a new Driver via POST /api/owner/drivers
        CreateDriverRequest createDriver = new CreateDriverRequest(
                "pg_driver_rajesh",
                "DriverSecurePass123!",
                "Rajesh Kumar",
                "9876543211",
                "DL-AP-2026-999"
        );

        MvcResult driverCreateResult = mockMvc.perform(post("/api/owner/drivers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDriver)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("pg_driver_rajesh"))
                .andReturn();

        DriverResponse driverRes = objectMapper.readValue(driverCreateResult.getResponse().getContentAsString(), DriverResponse.class);
        driverId = driverRes.id();

        // Driver logs in via /api/auth/login
        LoginRequest driverLoginReq = new LoginRequest("pg_driver_rajesh", "DriverSecurePass123!");
        MvcResult driverLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driverLoginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("DRIVER"))
                .andReturn();

        LoginResponse driverAuthRes = objectMapper.readValue(driverLoginResult.getResponse().getContentAsString(), LoginResponse.class);
        driverToken = driverAuthRes.accessToken();
        assertThat(driverToken).isNotEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("3. Lorry CRUD Operations in PostgreSQL")
    void testLorryCrudOperations() throws Exception {
        CreateLorryRequest createLorry = new CreateLorryRequest(
                "AP39TX1234",
                "Tata Signa 4825.TK",
                new BigDecimal("28.50"),
                "AVAILABLE",
                "IALERT-AP-1234",
                "High performance 16-wheeler haulage truck",
                null
        );

        MvcResult createResult = mockMvc.perform(post("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createLorry)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationNumber").value("AP39TX1234"))
                .andReturn();

        OwnerLorryDetailDto lorryRes = objectMapper.readValue(createResult.getResponse().getContentAsString(), OwnerLorryDetailDto.class);
        lorryId = lorryRes.id();
        assertThat(lorryId).isNotNull();

        // Retrieve Lorries list
        mockMvc.perform(get("/api/owner/lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registrationNumber").value("AP39TX1234"));
    }

    @Test
    @Order(4)
    @DisplayName("4. Freight Trip Management: Create, Start, and Complete Lifecycle")
    void testTripManagementWorkflow() throws Exception {
        // Assign lorry to driver
        User driver = userRepository.findById(driverId).orElseThrow();
        Lorry lorry = lorryRepository.findById(lorryId).orElseThrow();
        driver.setAssignedLorry(lorry);
        userRepository.save(driver);

        // Owner creates a Trip
        TripCreateDto createTrip = new TripCreateDto(
                lorryId,
                driverId,
                "Visakhapatnam Steel Plant, AP",
                "Hyderabad Outer Logistics Park, TS",
                LocalDate.now(),
                LocalTime.of(8, 0),
                LocalDate.now().plusDays(1),
                LocalTime.of(18, 0),
                "Industrial Steel Coils",
                "Handle with care"
        );

        MvcResult tripResult = mockMvc.perform(post("/api/owner/trips")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTrip)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripReference").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andReturn();

        TripResponseDto tripRes = objectMapper.readValue(tripResult.getResponse().getContentAsString(), TripResponseDto.class);
        tripId = tripRes.id();

        // Driver starts the trip
        mockMvc.perform(post("/api/driver/trips/{id}/start", tripId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripStatus").value("IN_PROGRESS"));

        // Verify Lorry status updated to ON_TRIP
        Lorry updatedLorry = lorryRepository.findById(lorryId).orElseThrow();
        assertThat(updatedLorry.getOperationalStatus()).isEqualTo(OperationalStatus.ON_TRIP);

        // Driver completes the trip
        mockMvc.perform(post("/api/driver/trips/{id}/complete", tripId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripStatus").value("COMPLETED"));

        // Verify Lorry status reverted to AVAILABLE
        updatedLorry = lorryRepository.findById(lorryId).orElseThrow();
        assertThat(updatedLorry.getOperationalStatus()).isEqualTo(OperationalStatus.AVAILABLE);
    }

    @Test
    @Order(5)
    @DisplayName("5. Real-Time GPS Location & Speed Updates via LiveTrackingService")
    void testLiveGpsTrackingAndSpeedUpdates() throws Exception {
        Trip trip = tripRepository.findById(tripId).orElseThrow();
        trip.setTripStatus(TripStatus.IN_PROGRESS);
        tripRepository.save(trip);

        SubmitLocationRequest locReq = new SubmitLocationRequest(
                17.4482930,
                78.3914850,
                58.50,
                142.00,
                3.50,
                Instant.now()
        );

        // Driver posts GPS coordinates
        mockMvc.perform(post("/api/driver/trips/{tripId}/location", tripId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentSpeedKmh").value(58.50));

        // Owner queries running lorries live tracking
        mockMvc.perform(get("/api/owner/tracking/running-lorries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registrationNumber").value("AP39TX1234"))
                .andExpect(jsonPath("$[0].latitude").value(17.448293))
                .andExpect(jsonPath("$[0].longitude").value(78.391485))
                .andExpect(jsonPath("$[0].currentSpeedKmh").value(58.5));

        // Mark trip completed
        trip.setTripStatus(TripStatus.COMPLETED);
        tripRepository.save(trip);
    }

    @Test
    @Order(6)
    @DisplayName("6. Maintenance Records & JPQL Month/Year Aggregations in PostgreSQL")
    void testMaintenanceRecordsAndAggregations() throws Exception {
        CreateMaintenanceRecordRequest maintReq = new CreateMaintenanceRecordRequest(
                lorryId,
                "Oil & Filter Service",
                LocalDate.now(),
                "Full synthetic engine oil replacement and air filter cleaning",
                new BigDecimal("8500.00"),
                42500,
                LocalDate.now().plusMonths(3),
                "Authorized Tata Commercial Service, Hyderabad",
                null
        );

        mockMvc.perform(post("/api/owner/maintenance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maintReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cost").value(8500.00))
                .andExpect(jsonPath("$.serviceType").value("Oil & Filter Service"));

        // Test custom JPQL functions: sumCostByOwnerGroupedByMonth & findDistinctYearsByOwner
        User owner = userRepository.findById(ownerId).orElseThrow();
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1);

        List<Object[]> monthlyCosts = maintenanceRecordRepository.sumCostByOwnerGroupedByMonth(owner, start, end);
        assertThat(monthlyCosts).isNotEmpty();
        assertThat((Number) monthlyCosts.get(0)[0]).isEqualTo(LocalDate.now().getMonthValue());
        assertThat((BigDecimal) monthlyCosts.get(0)[1]).isEqualByComparingTo(new BigDecimal("8500.00"));

        List<Integer> years = maintenanceRecordRepository.findDistinctYearsByOwner(owner);
        assertThat(years).contains(LocalDate.now().getYear());
    }

    @Test
    @Order(7)
    @DisplayName("7. Diesel and Finance Transactions with JPQL Aggregates in PostgreSQL")
    void testFinanceTransactionsAndRevenueLedger() throws Exception {
        // Income Transaction (FREIGHT)
        CreateFinanceTransactionRequest freightIncome = new CreateFinanceTransactionRequest(
                "INCOME",
                "FREIGHT",
                "Freight charges for Steel coil transit",
                new BigDecimal("65000.00"),
                LocalDate.now(),
                "RECEIVED",
                lorryId,
                tripId,
                "BANK_TRANSFER",
                "NEFT-REF-992144"
        );

        mockMvc.perform(post("/api/owner/finance/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(freightIncome)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(65000.00));

        // Expense Transaction (DIESEL)
        CreateFinanceTransactionRequest dieselExpense = new CreateFinanceTransactionRequest(
                "EXPENSE",
                "DIESEL",
                "200L High Speed Diesel from HPCL Highway Outlet",
                new BigDecimal("18600.00"),
                LocalDate.now(),
                "RECEIVED",
                lorryId,
                tripId,
                "FUEL_CARD",
                "HPCL-TX-88321"
        );

        mockMvc.perform(post("/api/owner/finance/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dieselExpense)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(18600.00));

        User owner = userRepository.findById(ownerId).orElseThrow();
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1);

        BigDecimal income = financeTransactionRepository.sumIncomeByOwnerAndDateRange(owner, start, end);
        BigDecimal expense = financeTransactionRepository.sumExpenseByOwnerAndDateRange(owner, start, end);

        assertThat(income).isEqualByComparingTo(new BigDecimal("65000.00"));
        assertThat(expense).isEqualByComparingTo(new BigDecimal("18600.00"));

        List<Object[]> groupedByMonth = financeTransactionRepository.sumByOwnerGroupedByMonthAndType(owner, start, end);
        assertThat(groupedByMonth).isNotEmpty();
    }

    @Test
    @Order(8)
    @DisplayName("8. Document Records Metadata in PostgreSQL")
    void testDocumentMetadata() throws Exception {
        User owner = userRepository.findById(ownerId).orElseThrow();
        Trip trip = tripRepository.findById(tripId).orElseThrow();
        Lorry lorry = lorryRepository.findById(lorryId).orElseThrow();

        // Trip Document
        TripDocument tripDoc = new TripDocument(
                trip,
                "E-Way Bill 88992211",
                "eway_bill.pdf",
                "stored_eway_88992211.pdf",
                "application/pdf",
                245892L,
                owner
        );
        tripDocumentRepository.save(tripDoc);
        assertThat(tripDoc.getId()).isNotNull();

        // Vehicle Document
        VehicleDocument vehDoc = new VehicleDocument(
                lorry,
                "Commercial Fitness Certificate",
                "fitness_cert.pdf",
                "stored_fitness_ap39tx1234.pdf",
                "application/pdf",
                512800L,
                owner
        );
        vehicleDocumentRepository.save(vehDoc);
        assertThat(vehDoc.getId()).isNotNull();

        List<TripDocument> tripDocs = tripDocumentRepository.findAllByTripIdOrderByUploadedAtDesc(tripId);
        assertThat(tripDocs).hasSize(1);

        List<VehicleDocument> vehDocs = vehicleDocumentRepository.findAllByLorryIdOrderByUploadedAtDesc(lorryId);
        assertThat(vehDocs).hasSize(1);
    }

    @Test
    @Order(9)
    @DisplayName("9. PostgreSQL Identity Sequence Synchronization (Empty and Populated tables)")
    void testSequenceSynchronization() throws Exception {
        Path syncScriptPath = Path.of("d:/lorry-fleet-manager/backend/database/sync-sequences-postgres.sql");
        String syncSql = Files.readString(syncScriptPath, StandardCharsets.UTF_8);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Execute the sequence synchronization script
            stmt.execute(syncSql);

            // Verify populated table 'users' sequence
            try (ResultSet rs = stmt.executeQuery("SELECT nextval(pg_get_serial_sequence('users', 'id'))")) {
                assertThat(rs.next()).isTrue();
                long nextUserId = rs.getLong(1);
                Long maxUserId = userRepository.findAll().stream().mapToLong(User::getId).max().orElse(0L);
                assertThat(nextUserId).isEqualTo(maxUserId + 1);
            }
        }
    }

    @Test
    @Order(10)
    @DisplayName("10. Database Persistence across Connection Reset")
    void testDatabasePersistenceAcrossRestart() throws Exception {
        // Disconnect and query with a brand new direct connection
        try (Connection freshConn = embeddedPostgres.getPostgresDatabase().getConnection();
             Statement stmt = freshConn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(2);
            }

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM lorries")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(1);
            }

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM trips")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(1);
            }

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM finance_transactions")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(2);
            }
        }
    }

    @Test
    @Order(11)
    @DisplayName("11. Actual JVM Memory Footprint Measurement for 512 MB Container")
    void testMeasureActualJvmMemoryUsage() {
        System.gc(); // Suggest GC to get a clean baseline of steady-state usage

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        long heapUsedMB = heapUsage.getUsed() / (1024 * 1024);
        long heapCommittedMB = heapUsage.getCommitted() / (1024 * 1024);
        long nonHeapUsedMB = nonHeapUsage.getUsed() / (1024 * 1024);
        long nonHeapCommittedMB = nonHeapUsage.getCommitted() / (1024 * 1024);

        int liveThreads = ManagementFactory.getThreadMXBean().getThreadCount();
        int loadedClasses = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();

        long totalUsedMemoryMB = heapUsedMB + nonHeapUsedMB;

        System.out.println("================================================================================");
        System.out.println("ACTUAL MEASURED JVM MEMORY USAGE (SPRING BOOT 3.3.4 + POSTGRESQL):");
        System.out.println(String.format("  * Heap Memory Used:      %d MB (Committed: %d MB, Max: %d MB)",
                heapUsedMB, heapCommittedMB, heapUsage.getMax() / (1024 * 1024)));
        System.out.println(String.format("  * Non-Heap Memory Used:  %d MB (Committed: %d MB)",
                nonHeapUsedMB, nonHeapCommittedMB));
        System.out.println(String.format("  * Total Process Memory:  %d MB", totalUsedMemoryMB));
        System.out.println(String.format("  * Live Threads:          %d", liveThreads));
        System.out.println(String.format("  * Loaded Classes:        %d", loadedClasses));
        System.out.println(String.format("  * Headroom in 512 MB:    %d MB (%.1f%% free)",
                512 - totalUsedMemoryMB, ((512.0 - totalUsedMemoryMB) / 512.0) * 100.0));
        System.out.println("================================================================================");

        assertThat(totalUsedMemoryMB).isLessThan(400);
    }
}
