-- ===================================================================
-- Project: Lorry Fleet Manager
-- Script: create-database.sql
-- Purpose: Initial database creation for the Lorry Owner backend
-- Target: MySQL Server 8.4 LTS / MySQL 8.0+
-- Database Name: lorry_fleet_db
-- Encoding: utf8mb4 (Full Unicode support including multilingual characters and emojis)
-- Collation: utf8mb4_unicode_ci (Case-insensitive Unicode comparison)
-- ===================================================================

-- Create the database only if it does not already exist.
-- SAFE OPERATION: This command will NOT drop, delete, or overwrite any existing database.
CREATE DATABASE IF NOT EXISTS lorry_fleet_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Note on tables:
-- Tables are intentionally omitted in this foundational step.
-- Application tables will be introduced in subsequent milestones once
-- business requirements and entity models are finalized.
