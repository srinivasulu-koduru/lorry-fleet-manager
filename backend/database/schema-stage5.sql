-- ==============================================================================
-- BHP Logistics Fleet Manager - Database Schema Migration Stage 5
-- Additive Migration: Planned Delivery Time & Maintenance Records
-- Safe for production MySQL 8.4; spring.jpa.hibernate.ddl-auto=validate
-- ==============================================================================

USE lorry_fleet_db;

-- 1. Add planned_delivery_time to trips if not already present
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'lorry_fleet_db' 
      AND TABLE_NAME = 'trips' 
      AND COLUMN_NAME = 'planned_delivery_time'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE trips ADD COLUMN planned_delivery_time TIMESTAMP NULL', 
    'SELECT "planned_delivery_time already exists"'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Create maintenance_records table if not exists
CREATE TABLE IF NOT EXISTS maintenance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lorry_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    maintenance_date DATE NOT NULL,
    service_type VARCHAR(100) NOT NULL,
    description TEXT NULL,
    cost DECIMAL(10, 2) NOT NULL,
    service_center VARCHAR(150) NULL,
    odometer_reading_km INT NULL,
    next_service_date DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_maintenance_lorry FOREIGN KEY (lorry_id) REFERENCES lorries(id),
    CONSTRAINT fk_maintenance_owner FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Associate unassigned fleet lorries with owner 1 (fleet_owner)
UPDATE lorries SET owner_id = 1 WHERE owner_id IS NULL;

-- 4. Assign driver Ramesh to lorry 1 (AP 31 TC 1234) if unassigned
UPDATE users SET assigned_lorry_id = 1 WHERE id = 2 AND assigned_lorry_id IS NULL;

-- 5. Seed realistic maintenance records for lorry 1 (Total Maintenance Cost = ₹ 48,750 matching reference UI)
INSERT INTO maintenance_records (lorry_id, owner_id, maintenance_date, service_type, description, cost, service_center, odometer_reading_km, next_service_date)
SELECT 1, 1, '2025-09-15', 'Regular Service', 'Engine oil change, filter replacement', 8500.00, 'SRS Auto Center', 45230, '2025-12-15'
WHERE NOT EXISTS (SELECT 1 FROM maintenance_records WHERE lorry_id = 1 AND service_type = 'Regular Service');

INSERT INTO maintenance_records (lorry_id, owner_id, maintenance_date, service_type, description, cost, service_center, odometer_reading_km, next_service_date)
SELECT 1, 1, '2025-06-10', 'Brake Service', 'Brake pad replacement', 12000.00, 'Sai Motors', 38760, '2025-09-10'
WHERE NOT EXISTS (SELECT 1 FROM maintenance_records WHERE lorry_id = 1 AND service_type = 'Brake Service');

INSERT INTO maintenance_records (lorry_id, owner_id, maintenance_date, service_type, description, cost, service_center, odometer_reading_km, next_service_date)
SELECT 1, 1, '2025-03-22', 'General Inspection', 'Full vehicle checkup', 6250.00, 'Ravi Garage', 32450, '2025-06-22'
WHERE NOT EXISTS (SELECT 1 FROM maintenance_records WHERE lorry_id = 1 AND service_type = 'General Inspection');

INSERT INTO maintenance_records (lorry_id, owner_id, maintenance_date, service_type, description, cost, service_center, odometer_reading_km, next_service_date)
SELECT 1, 1, '2025-01-15', 'Tyre Replacement', '4 rear tyres replaced', 22000.00, 'MRF Tyre Plaza', 28100, '2025-07-15'
WHERE NOT EXISTS (SELECT 1 FROM maintenance_records WHERE lorry_id = 1 AND service_type = 'Tyre Replacement');

-- 6. Update in-progress trip (TRIP-2026-1003) with start time and planned delivery time for realistic ETA calculation
UPDATE trips 
SET start_time = '2026-10-27 08:30:00', 
    planned_delivery_time = DATE_ADD(NOW(), INTERVAL 4 HOUR)
WHERE trip_reference = 'TRIP-2026-1003' AND planned_delivery_time IS NULL;
