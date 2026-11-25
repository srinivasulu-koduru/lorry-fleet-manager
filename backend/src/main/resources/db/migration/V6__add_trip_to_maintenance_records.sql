-- Migration: Add trip_id foreign key to maintenance_records table
-- Idempotent, safe, and non-destructive for PostgreSQL production database

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'maintenance_records' AND column_name = 'trip_id'
    ) THEN
        ALTER TABLE maintenance_records
        ADD COLUMN trip_id BIGINT NULL;
    END IF;
END $$;

-- Add foreign key constraint referencing trips(id) with ON DELETE SET NULL
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_maintenance_records_trip' AND table_name = 'maintenance_records'
    ) THEN
        ALTER TABLE maintenance_records
        ADD CONSTRAINT fk_maintenance_records_trip
        FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Create index for trip-wise maintenance queries
CREATE INDEX IF NOT EXISTS idx_maintenance_trip ON maintenance_records(trip_id);
