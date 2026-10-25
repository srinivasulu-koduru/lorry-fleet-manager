package com.lorryowner.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity representing a commercial freight vehicle in the fleet.
 * Maps to the 'lorries' table in MySQL lorry_fleet_db.
 */
@Entity
@Table(name = "lorries")
public class Lorry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", nullable = false, unique = true, length = 20)
    private String registrationNumber;

    @Column(name = "make_model", nullable = false, length = 100)
    private String makeModel;

    @Column(name = "capacity_tons", nullable = false, precision = 5, scale = 2)
    private BigDecimal capacityTons;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 25)
    private OperationalStatus operationalStatus = OperationalStatus.AVAILABLE;

    @Column(name = "ialert_unit_id", length = 50)
    private String ialertUnitId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Lorry() {
    }

    public Lorry(String registrationNumber, String makeModel, BigDecimal capacityTons, OperationalStatus operationalStatus) {
        this.registrationNumber = registrationNumber;
        this.makeModel = makeModel;
        this.capacityTons = capacityTons;
        this.operationalStatus = operationalStatus != null ? operationalStatus : OperationalStatus.AVAILABLE;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getMakeModel() {
        return makeModel;
    }

    public void setMakeModel(String makeModel) {
        this.makeModel = makeModel;
    }

    public BigDecimal getCapacityTons() {
        return capacityTons;
    }

    public void setCapacityTons(BigDecimal capacityTons) {
        this.capacityTons = capacityTons;
    }

    public OperationalStatus getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(OperationalStatus operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public String getIalertUnitId() {
        return ialertUnitId;
    }

    public void setIalertUnitId(String ialertUnitId) {
        this.ialertUnitId = ialertUnitId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lorry lorry = (Lorry) o;
        return Objects.equals(id, lorry.id) || Objects.equals(registrationNumber, lorry.registrationNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registrationNumber);
    }
}
