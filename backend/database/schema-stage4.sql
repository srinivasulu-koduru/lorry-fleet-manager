-- ===================================================================
-- Lorry Fleet Manager - Stage 4 Database Schema Migration
-- Script: schema-stage4.sql
-- Target Database: lorry_fleet_db (MySQL 8.4 LTS / MySQL 8.0+)
-- Purpose: Introduce historical trips table and relationships
--          to support Driver Trip History, Filtering, and Performance analytics.
-- Safe & Idempotent: Uses CREATE TABLE IF NOT EXISTS and INSERT IGNORE.
-- Preserves existing tables and records; 100% backward compatible.
-- ===================================================================

USE lorry_fleet_db;

CREATE TABLE IF NOT EXISTS trips (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_reference VARCHAR(50) NOT NULL UNIQUE,
    driver_id BIGINT NOT NULL,
    lorry_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    origin_location VARCHAR(150) NOT NULL,
    destination_location VARCHAR(150) NOT NULL,
    trip_status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    trip_date DATE NOT NULL,
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    distance_km DECIMAL(8,2) NULL,
    delivery_notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_trips_driver FOREIGN KEY (driver_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_trips_lorry FOREIGN KEY (lorry_id) REFERENCES lorries(id) ON DELETE RESTRICT,
    CONSTRAINT fk_trips_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_trips_driver (driver_id),
    INDEX idx_trips_lorry (lorry_id),
    INDEX idx_trips_owner (owner_id),
    INDEX idx_trips_date (trip_date),
    INDEX idx_trips_status (trip_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optional initial seed trips for existing driver Ramesh (id=2), owner (id=1), lorries (1, 3)
INSERT IGNORE INTO trips (
    trip_reference, driver_id, lorry_id, owner_id,
    origin_location, destination_location, trip_status,
    trip_date, start_time, end_time, distance_km, delivery_notes
) VALUES
(
    'TRIP-2026-1001', 2, 1, 1,
    'Visakhapatnam Port, AP', 'Vijayawada Commercial Hub, AP', 'COMPLETED',
    '2026-10-20', '2026-10-20 06:30:00', '2026-10-20 14:45:00', 350.50,
    'Steel consignment safely delivered to terminal.'
),
(
    'TRIP-2026-1002', 2, 3, 1,
    'Vijayawada Commercial Hub, AP', 'Hyderabad Logistics Park, TS', 'COMPLETED',
    '2026-10-22', '2026-10-22 08:00:00', '2026-10-22 15:30:00', 275.00,
    'Agricultural produce delivered to warehouse.'
),
(
    'TRIP-2026-1003', 2, 1, 1,
    'Hyderabad Logistics Park, TS', 'Nellore Distribution Center, AP', 'IN_PROGRESS',
    '2026-10-27', '2026-10-27 10:15:00', NULL, NULL,
    'Heavy machinery transit currently underway.'
);
