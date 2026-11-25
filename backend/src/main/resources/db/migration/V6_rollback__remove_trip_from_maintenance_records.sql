-- Rollback Migration: Safely remove trip_id column and foreign key constraint
-- For emergency rollback only if needed

DROP INDEX IF EXISTS idx_maintenance_trip;

ALTER TABLE maintenance_records
DROP CONSTRAINT IF EXISTS fk_maintenance_records_trip;

ALTER TABLE maintenance_records
DROP COLUMN IF EXISTS trip_id;
