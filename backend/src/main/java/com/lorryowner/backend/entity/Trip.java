package com.lorryowner.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_reference", nullable = false, unique = true, length = 50)
    private String tripReference;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lorry_id", nullable = false)
    private Lorry lorry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "origin_location", nullable = false, length = 150)
    private String originLocation;

    @Column(name = "destination_location", nullable = false, length = 150)
    private String destinationLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_status", nullable = false, length = 30)
    private TripStatus tripStatus = TripStatus.COMPLETED;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "distance_km", precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "planned_delivery_time")
    private Instant plannedDeliveryTime;

    @Column(name = "load_type", length = 100)
    private String loadType;

    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Trip() {}

    public Trip(
            String tripReference,
            User driver,
            Lorry lorry,
            User owner,
            String originLocation,
            String destinationLocation,
            TripStatus tripStatus,
            LocalDate tripDate,
            Instant startTime,
            Instant endTime,
            BigDecimal distanceKm,
            String deliveryNotes
    ) {
        this.tripReference = tripReference;
        this.driver = driver;
        this.lorry = lorry;
        this.owner = owner;
        this.originLocation = originLocation;
        this.destinationLocation = destinationLocation;
        this.tripStatus = tripStatus;
        this.tripDate = tripDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.distanceKm = distanceKm;
        this.deliveryNotes = deliveryNotes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTripReference() {
        return tripReference;
    }

    public void setTripReference(String tripReference) {
        this.tripReference = tripReference;
    }

    public User getDriver() {
        return driver;
    }

    public void setDriver(User driver) {
        this.driver = driver;
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

    public String getOriginLocation() {
        return originLocation;
    }

    public void setOriginLocation(String originLocation) {
        this.originLocation = originLocation;
    }

    public String getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(String destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public TripStatus getTripStatus() {
        return tripStatus;
    }

    public void setTripStatus(TripStatus tripStatus) {
        this.tripStatus = tripStatus;
    }

    public LocalDate getTripDate() {
        return tripDate;
    }

    public void setTripDate(LocalDate tripDate) {
        this.tripDate = tripDate;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Instant getPlannedDeliveryTime() {
        return plannedDeliveryTime;
    }

    public void setPlannedDeliveryTime(Instant plannedDeliveryTime) {
        this.plannedDeliveryTime = plannedDeliveryTime;
    }

    public String getLoadType() {
        return loadType;
    }

    public void setLoadType(String loadType) {
        this.loadType = loadType;
    }

    public String getDeliveryNotes() {
        return deliveryNotes;
    }

    public void setDeliveryNotes(String deliveryNotes) {
        this.deliveryNotes = deliveryNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
