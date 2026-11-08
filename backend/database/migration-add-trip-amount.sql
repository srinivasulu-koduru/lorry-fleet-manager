-- ==============================================================================
-- Idempotent Migration: Add trip_amount column to trips table
-- Safe for existing production data. Preserves all existing trips and transactions.
-- ==============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'trips' AND column_name = 'trip_amount'
    ) THEN
        ALTER TABLE trips ADD COLUMN trip_amount NUMERIC(12, 2) DEFAULT 0.00;
        RAISE NOTICE 'Added column trip_amount NUMERIC(12, 2) to trips table.';
    ELSE
        RAISE NOTICE 'Column trip_amount already exists in trips table.';
    END IF;
END $$;
