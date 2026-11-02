-- ==============================================================================
-- BHP Logistics Fleet Manager - Database Schema Migration Stage 9
-- Finance Module: Authoritative Financial Transactions & Tracking
-- Safe for production MySQL 8.4; spring.jpa.hibernate.ddl-auto=validate
-- ==============================================================================

USE lorry_fleet_db;

-- 1. Create finance_transactions table if not exists
CREATE TABLE IF NOT EXISTS finance_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    lorry_id BIGINT NULL,
    trip_id BIGINT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    transaction_date DATE NOT NULL,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    payment_mode VARCHAR(50) NULL,
    reference_number VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ft_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ft_lorry FOREIGN KEY (lorry_id) REFERENCES lorries (id) ON DELETE SET NULL,
    CONSTRAINT fk_ft_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE SET NULL,
    INDEX idx_ft_owner_date (owner_id, transaction_date DESC),
    INDEX idx_ft_owner_type_date (owner_id, transaction_type, transaction_date),
    INDEX idx_ft_lorry_date (lorry_id, transaction_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
