-- ==============================================================================
-- BHP Logistics Fleet Manager - Database Schema Migration Stage 8
-- Additive Migration: Performance Indexes for Maintenance Date Range Queries
-- Safe for production MySQL 8.4; spring.jpa.hibernate.ddl-auto=validate
-- ==============================================================================

USE lorry_fleet_db;

-- 1. Create index on (owner_id, maintenance_date) for monthly expense calculations
SET @idx_exists = (
    SELECT COUNT(*) 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = 'lorry_fleet_db' 
      AND TABLE_NAME = 'maintenance_records' 
      AND INDEX_NAME = 'idx_maintenance_owner_date'
);
SET @sql = IF(@idx_exists = 0, 
    'CREATE INDEX idx_maintenance_owner_date ON maintenance_records(owner_id, maintenance_date)', 
    'SELECT "idx_maintenance_owner_date already exists"'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Create index on (lorry_id, maintenance_date) for lorry history chronological queries
SET @idx_exists2 = (
    SELECT COUNT(*) 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = 'lorry_fleet_db' 
      AND TABLE_NAME = 'maintenance_records' 
      AND INDEX_NAME = 'idx_maintenance_lorry_date'
);
SET @sql2 = IF(@idx_exists2 = 0, 
    'CREATE INDEX idx_maintenance_lorry_date ON maintenance_records(lorry_id, maintenance_date)', 
    'SELECT "idx_maintenance_lorry_date already exists"'
);
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
