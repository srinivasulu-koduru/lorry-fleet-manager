package com.lorryowner.backend.controller;

import com.lorryowner.backend.entity.*;
import com.lorryowner.backend.repository.*;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LorryRepository lorryRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripDocumentRepository tripDocumentRepository;

    @Autowired
    private VehicleDocumentRepository vehicleDocumentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User owner1;
    private User owner2;
    private User driver1;
    private User driver2;
    private Lorry lorry1;
    private Lorry lorry2;
    private Trip trip1;

    private String owner1Token;
    private String owner2Token;
    private String driver1Token;
    private String driver2Token;

    @BeforeEach
    void setUp() {
        tearDown();

        owner1 = new User("doc_owner1", passwordEncoder.encode("Password123!"), Role.OWNER, "Owner One", "9876543210", null);
        userRepository.save(owner1);

        owner2 = new User("doc_owner2", passwordEncoder.encode("Password123!"), Role.OWNER, "Owner Two", "9876543211", null);
        userRepository.save(owner2);

        lorry1 = new Lorry("AP 02 AB 1234", "Tata Prima 16", new BigDecimal("16.00"), OperationalStatus.AVAILABLE);
        lorry1.setOwner(owner1);
        lorryRepository.save(lorry1);

        lorry2 = new Lorry("TS 09 EF 5678", "Ashok Leyland", new BigDecimal("20.00"), OperationalStatus.AVAILABLE);
        lorry2.setOwner(owner2);
        lorryRepository.save(lorry2);

        driver1 = new User("doc_driver1", passwordEncoder.encode("Password123!"), Role.DRIVER, "Driver One", "9876543212", "DL12345");
        driver1.setOwner(owner1);
        driver1.setAssignedLorry(lorry1);
        userRepository.save(driver1);

        driver2 = new User("doc_driver2", passwordEncoder.encode("Password123!"), Role.DRIVER, "Driver Two", "9876543213", "DL12346");
        driver2.setOwner(owner2);
        driver2.setAssignedLorry(lorry2);
        userRepository.save(driver2);

        trip1 = new Trip(
                "TRIP-DOC-001",
                driver1,
                lorry1,
                owner1,
                "Nellore",
                "Ananthapur",
                TripStatus.IN_PROGRESS,
                LocalDate.now(),
                null,
                null,
                new BigDecimal("350.00"),
                "High priority cargo"
        );
        tripRepository.save(trip1);

        owner1Token = jwtService.generateToken(owner1);
        owner2Token = jwtService.generateToken(owner2);
        driver1Token = jwtService.generateToken(driver1);
        driver2Token = jwtService.generateToken(driver2);
    }

    @AfterEach
    void tearDown() {
        tripDocumentRepository.deleteAll();
        vehicleDocumentRepository.deleteAll();
        tripRepository.deleteAll();
        userRepository.findAll().forEach(u -> {
            u.setAssignedLorry(null);
            userRepository.save(u);
        });
        userRepository.deleteAll();
        lorryRepository.deleteAll();
    }

    @Test
    @DisplayName("Owner can upload, list, download, and delete trip documents successfully")
    void ownerTripDocumentLifecycle_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "eway_bill_123.pdf",
                "application/pdf",
                "%PDF-1.4 test trip document content".getBytes()
        );

        // 1. Upload
        String responseStr = mockMvc.perform(multipart("/api/owner/trips/" + trip1.getId() + "/documents")
                        .file(file)
                        .param("documentName", "E-Way Bill")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentName", is("E-Way Bill")))
                .andExpect(jsonPath("$.originalFilename", is("eway_bill_123.pdf")))
                .andExpect(jsonPath("$.contentType", is("application/pdf")))
                .andReturn().getResponse().getContentAsString();

        // Extract ID
        org.json.JSONObject json = new org.json.JSONObject(responseStr);
        long docId = json.getLong("id");

        // 2. List
        mockMvc.perform(get("/api/owner/trips/" + trip1.getId() + "/documents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].documentName", is("E-Way Bill")));

        // 3. Download
        mockMvc.perform(get("/api/owner/trips/" + trip1.getId() + "/documents/" + docId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("eway_bill_123.pdf")));

        // 4. Delete
        mockMvc.perform(delete("/api/owner/trips/" + trip1.getId() + "/documents/" + docId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isNoContent());

        // Verify list empty
        mockMvc.perform(get("/api/owner/trips/" + trip1.getId() + "/documents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Assigned driver can list and download trip documents, but unrelated driver cannot")
    void driverTripDocumentAccess_AuthorizationEnforced() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        );

        String uploadJson = mockMvc.perform(multipart("/api/owner/trips/" + trip1.getId() + "/documents")
                        .file(file)
                        .param("documentName", "Tax Invoice")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long docId = new org.json.JSONObject(uploadJson).getLong("id");

        // Assigned driver (driver1) can list
        mockMvc.perform(get("/api/driver/trips/" + trip1.getId() + "/documents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].documentName", is("Tax Invoice")));

        // Assigned driver (driver1) can download
        mockMvc.perform(get("/api/driver/trips/" + trip1.getId() + "/documents/" + docId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token))
                .andExpect(status().isOk());

        // Unassigned driver (driver2) CANNOT list
        mockMvc.perform(get("/api/driver/trips/" + trip1.getId() + "/documents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver2Token))
                .andExpect(status().isNotFound());

        // Unassigned driver (driver2) CANNOT download
        mockMvc.perform(get("/api/driver/trips/" + trip1.getId() + "/documents/" + docId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver2Token))
                .andExpect(status().isNotFound());

        // Driver CANNOT upload trip documents (Role-guard check)
        mockMvc.perform(multipart("/api/owner/trips/" + trip1.getId() + "/documents")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Owner can upload, list, download, and delete vehicle documents successfully")
    void ownerVehicleDocumentLifecycle_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rc_card.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
        );

        // Upload
        String uploadJson = mockMvc.perform(multipart("/api/owner/lorries/" + lorry1.getId() + "/documents")
                        .file(file)
                        .param("documentName", "Registration Certificate (RC)")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentName", is("Registration Certificate (RC)")))
                .andExpect(jsonPath("$.originalFilename", is("rc_card.png")))
                .andReturn().getResponse().getContentAsString();

        long docId = new org.json.JSONObject(uploadJson).getLong("id");

        // List
        mockMvc.perform(get("/api/owner/lorries/" + lorry1.getId() + "/documents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Download
        mockMvc.perform(get("/api/owner/lorries/" + lorry1.getId() + "/documents/" + docId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isOk());

        // Other owner (owner2) cannot access
        mockMvc.perform(get("/api/owner/lorries/" + lorry1.getId() + "/documents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner2Token))
                .andExpect(status().isNotFound());

        // Assigned driver (driver1 assigned to lorry1) can list vehicle documents
        mockMvc.perform(get("/api/driver/lorries/" + lorry1.getId() + "/documents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Unassigned driver (driver2) cannot list lorry1 documents
        mockMvc.perform(get("/api/driver/lorries/" + lorry1.getId() + "/documents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driver2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Unsupported file type (.exe) is rejected with 400 Bad Request")
    void unsupportedFileFormat_Rejected() throws Exception {
        MockMultipartFile badFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/x-msdownload",
                "MZ...".getBytes()
        );

        mockMvc.perform(multipart("/api/owner/trips/" + trip1.getId() + "/documents")
                        .file(badFile)
                        .param("documentName", "Malware")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner1Token))
                .andExpect(status().isBadRequest());
    }
}
