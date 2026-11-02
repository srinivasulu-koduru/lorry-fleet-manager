-- ==============================================================================
-- BHP Logistics Fleet Manager - Database Schema Migration Stage 10
-- Trip Documents & Vehicle Documents Management Module
-- Safe for production MySQL 8.4; spring.jpa.hibernate.ddl-auto=validate
-- ==============================================================================

USE lorry_fleet_db;

-- 1. Create trip_documents table if not exists
CREATE TABLE IF NOT EXISTS trip_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    document_name VARCHAR(150) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_by_id BIGINT NOT NULL,
    CONSTRAINT fk_td_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    CONSTRAINT fk_td_uploader FOREIGN KEY (uploaded_by_id) REFERENCES users (id),
    INDEX idx_td_trip (trip_id),
    INDEX idx_td_uploader (uploaded_by_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Create vehicle_documents table if not exists
CREATE TABLE IF NOT EXISTS vehicle_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lorry_id BIGINT NOT NULL,
    document_name VARCHAR(150) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_by_id BIGINT NOT NULL,
    CONSTRAINT fk_vd_lorry FOREIGN KEY (lorry_id) REFERENCES lorries (id) ON DELETE CASCADE,
    CONSTRAINT fk_vd_uploader FOREIGN KEY (uploaded_by_id) REFERENCES users (id),
    INDEX idx_vd_lorry (lorry_id),
    INDEX idx_vd_uploader (uploaded_by_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
