-- ==============================================================================
-- BHP Logistics Fleet Manager - Database Schema Migration Stage 7
-- Live GPS Tracking for Active Trips and Lorries
-- Safe for production MySQL 8.4; spring.jpa.hibernate.ddl-auto=validate
-- ==============================================================================

USE lorry_fleet_db;

-- 1. Create trip_locations table if not exists
CREATE TABLE IF NOT EXISTS trip_locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    lorry_id BIGINT NOT NULL,
    driver_id BIGINT NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    speed_kmh DECIMAL(6, 2) NULL,
    heading DECIMAL(5, 2) NULL,
    accuracy_meters DECIMAL(6, 2) NULL,
    client_timestamp TIMESTAMP NULL,
    server_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tl_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    CONSTRAINT fk_tl_lorry FOREIGN KEY (lorry_id) REFERENCES lorries (id) ON DELETE CASCADE,
    CONSTRAINT fk_tl_driver FOREIGN KEY (driver_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_trip_locations_trip_time (trip_id, server_timestamp DESC),
    INDEX idx_trip_locations_lorry_time (lorry_id, server_timestamp DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Seed realistic live tracking coordinates for the currently active trip (TRIP-2026-1003)
-- Active Trip ID 10: Hyderabad Logistics Park to Nellore Distribution Center via Kurnool (NH 44)
-- Matches reference screenshot: Vehicle AP 31 TC 1234, Near Kurnool (NH 44), 58 km/h, 145 km travelled
INSERT INTO trip_locations (
    trip_id, lorry_id, driver_id, latitude, longitude, speed_kmh, heading, accuracy_meters, client_timestamp, server_timestamp
)
SELECT 10, 1, 2, 15.8281000, 78.0373000, 58.00, 165.50, 4.20, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM trip_locations WHERE trip_id = 10
);
