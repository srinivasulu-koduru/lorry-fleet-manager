package com.lorryowner.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lorry_id", nullable = false)
    private Lorry lorry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @Column(name = "service_type", nullable = false, length = 100)
    private String serviceType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "service_center", length = 150)
    private String serviceCenter;

    @Column(name = "odometer_reading_km")
    private Integer odometerReadingKm;

    @Column(name = "next_service_date")
    private LocalDate nextServiceDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MaintenanceRecord() {}

    public MaintenanceRecord(
            Lorry lorry,
            User owner,
            LocalDate maintenanceDate,
            String serviceType,
            String description,
            BigDecimal cost,
            String serviceCenter,
            Integer odometerReadingKm,
            LocalDate nextServiceDate
    ) {
        this.lorry = lorry;
        this.owner = owner;
        this.maintenanceDate = maintenanceDate;
        this.serviceType = serviceType;
        this.description = description;
        this.cost = cost;
        this.serviceCenter = serviceCenter;
        this.odometerReadingKm = odometerReadingKm;
        this.nextServiceDate = nextServiceDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Lorry getLorry() {
        return lorry;
    }

    public void setLorry(Lorry lorry) {
        this.lorry = lorry;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public LocalDate getMaintenanceDate() {
        return maintenanceDate;
    }

    public void setMaintenanceDate(LocalDate maintenanceDate) {
        this.maintenanceDate = maintenanceDate;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getServiceCenter() {
        return serviceCenter;
    }

    public void setServiceCenter(String serviceCenter) {
        this.serviceCenter = serviceCenter;
    }

    public Integer getOdometerReadingKm() {
        return odometerReadingKm;
    }

    public void setOdometerReadingKm(Integer odometerReadingKm) {
        this.odometerReadingKm = odometerReadingKm;
    }

    public LocalDate getNextServiceDate() {
        return nextServiceDate;
    }

    public void setNextServiceDate(LocalDate nextServiceDate) {
        this.nextServiceDate = nextServiceDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
