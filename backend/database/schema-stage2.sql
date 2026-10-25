-- ===================================================================
-- Lorry Fleet Manager - Stage 2 Database Schema Migration
-- Script: schema-stage2.sql
-- Target Database: lorry_fleet_db (MySQL 8.4 LTS / MySQL 8.0+)
-- Safe & Idempotent: Uses CREATE TABLE IF NOT EXISTS.
-- Preserves existing tables and records; non-destructive.
-- ===================================================================

USE lorry_fleet_db;

-- 1. Users Table (Owners & Drivers)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'OWNER', 'DRIVER'
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    license_number VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    password_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_role (role),
    INDEX idx_users_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Lorries Table (Fleet Commercial Vehicles)
CREATE TABLE IF NOT EXISTS lorries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    registration_number VARCHAR(20) NOT NULL UNIQUE,
    make_model VARCHAR(100) NOT NULL,
    capacity_tons DECIMAL(5,2) NOT NULL,
    operational_status VARCHAR(25) NOT NULL DEFAULT 'AVAILABLE', -- 'AVAILABLE', 'ON_TRIP', 'MAINTENANCE', 'UNAVAILABLE'
    ialert_unit_id VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_lorries_status (operational_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
