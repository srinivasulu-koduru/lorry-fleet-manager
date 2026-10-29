package com.lorryowner.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing an authenticated live GPS location update for an active trip.
 * Corresponds to the 'trip_locations' table in MySQL.
 */
@Entity
@Table(name = "trip_locations")
public class TripLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lorry_id", nullable = false)
    private Lorry lorry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "speed_kmh", precision = 6, scale = 2)
    private BigDecimal speedKmh;

    @Column(name = "heading", precision = 5, scale = 2)
    private BigDecimal heading;

    @Column(name = "accuracy_meters", precision = 6, scale = 2)
    private BigDecimal accuracyMeters;

    @Column(name = "client_timestamp")
    private Instant clientTimestamp;

    @CreationTimestamp
    @Column(name = "server_timestamp", nullable = false, updatable = false)
    private Instant serverTimestamp;

    public TripLocation() {
    }

    public TripLocation(
            Trip trip,
            Lorry lorry,
            User driver,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal speedKmh,
            BigDecimal heading,
            BigDecimal accuracyMeters,
            Instant clientTimestamp
    ) {
        this.trip = trip;
        this.lorry = lorry;
        this.driver = driver;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speedKmh = speedKmh;
        this.heading = heading;
        this.accuracyMeters = accuracyMeters;
        this.clientTimestamp = clientTimestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public Lorry getLorry() {
        return lorry;
    }

    public void setLorry(Lorry lorry) {
        this.lorry = lorry;
    }

    public User getDriver() {
        return driver;
    }

    public void setDriver(User driver) {
        this.driver = driver;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getSpeedKmh() {
        return speedKmh;
    }

    public void setSpeedKmh(BigDecimal speedKmh) {
        this.speedKmh = speedKmh;
    }

    public BigDecimal getHeading() {
        return heading;
    }

    public void setHeading(BigDecimal heading) {
        this.heading = heading;
    }

    public BigDecimal getAccuracyMeters() {
        return accuracyMeters;
    }

    public void setAccuracyMeters(BigDecimal accuracyMeters) {
        this.accuracyMeters = accuracyMeters;
    }

    public Instant getClientTimestamp() {
        return clientTimestamp;
    }

    public void setClientTimestamp(Instant clientTimestamp) {
        this.clientTimestamp = clientTimestamp;
    }

    public Instant getServerTimestamp() {
        return serverTimestamp;
    }

    public void setServerTimestamp(Instant serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }

    public Double getLatitudeAsDouble() {
        return latitude != null ? latitude.doubleValue() : null;
    }

    public Double getLongitudeAsDouble() {
        return longitude != null ? longitude.doubleValue() : null;
    }

    public Double getSpeedKmhAsDouble() {
        return speedKmh != null ? speedKmh.doubleValue() : null;
    }
}
