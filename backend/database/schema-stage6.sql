-- ==============================================================================
-- BHP Logistics Fleet Manager - Database Schema Migration Stage 6
-- Additive Migration: Load Type column for Trip Management
-- Safe for production MySQL 8.4; spring.jpa.hibernate.ddl-auto=validate
-- ==============================================================================

USE lorry_fleet_db;

-- 1. Add load_type to trips table if not already present
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'lorry_fleet_db' 
      AND TABLE_NAME = 'trips' 
      AND COLUMN_NAME = 'load_type'
);

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE trips ADD COLUMN load_type VARCHAR(100) NULL', 
    'SELECT "load_type already exists"'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Populate load_type for historical trips if currently NULL
UPDATE trips SET load_type = 'Industrial Goods' WHERE id = 8 AND load_type IS NULL;
UPDATE trips SET load_type = 'General Goods' WHERE id = 9 AND load_type IS NULL;
UPDATE trips SET load_type = 'Food and Groceries' WHERE id = 10 AND load_type IS NULL;
