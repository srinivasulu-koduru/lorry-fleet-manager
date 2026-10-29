-- ===================================================================
-- Lorry Fleet Manager - Stage 3 Database Schema Migration
-- Script: schema-stage3.sql
-- Target Database: lorry_fleet_db (MySQL 8.4 LTS / MySQL 8.0+)
-- Purpose: Add optional owner and assigned_lorry relationships to support
--          driver-to-lorry assignments and multi-owner authorization.
-- Safe & Idempotent: Uses conditional checks to ensure non-destructive execution.
-- Preserves existing tables and records; 100% backward compatible.
-- ===================================================================

USE lorry_fleet_db;

-- 1. Add owner_id to lorries if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'lorry_fleet_db' AND TABLE_NAME = 'lorries' AND COLUMN_NAME = 'owner_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE lorries ADD COLUMN owner_id BIGINT NULL, ADD CONSTRAINT fk_lorries_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Add assigned_lorry_id to users if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'lorry_fleet_db' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'assigned_lorry_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE users ADD COLUMN assigned_lorry_id BIGINT NULL, ADD CONSTRAINT fk_users_assigned_lorry FOREIGN KEY (assigned_lorry_id) REFERENCES lorries(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. Add owner_id to users if not exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'lorry_fleet_db' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'owner_id');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE users ADD COLUMN owner_id BIGINT NULL, ADD CONSTRAINT fk_users_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
