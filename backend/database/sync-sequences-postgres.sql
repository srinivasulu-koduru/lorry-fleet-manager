-- ==============================================================================
-- BHP Logistics Fleet Manager - PostgreSQL Identity Sequence Synchronization
-- Target: PostgreSQL 14+ (Voroa Free PostgreSQL / Supabase / Neon / Cloud PG)
--
-- Safely synchronizes all identity sequences after manual migrations, CSV loads,
-- or database restores with explicit ID values.
--
-- IDEMPOTENT & EMPTY-TABLE SAFE:
-- - If table has records: sequence is set to MAX(id) with is_called = TRUE,
--   so the next generated ID is MAX(id) + 1.
-- - If table is EMPTY: sequence is set to 1 with is_called = FALSE,
--   so the first generated ID is 1 (avoiding skipping ID 1).
-- ==============================================================================

SELECT setval(
    pg_get_serial_sequence('users', 'id'),
    COALESCE(MAX(id), 1),
    MAX(id) IS NOT NULL
) FROM users;

SELECT setval(
    pg_get_serial_sequence('lorries', 'id'),
    COALESCE(MAX(id), 1),
    MAX(id) IS NOT NULL
) FROM lorries;

SELECT setval(
    pg_get_serial_sequence('trips', 'id'),
    COALESCE(MAX(id), 1),
    MAX(id) IS NOT NULL
) FROM trips;

SELECT setval(
    pg_get_serial_sequence('trip_locations', 'id'),
    COALESCE(MAX(id), 1),
    MAX(id) IS NOT NULL
) FROM trip_locations;

SELECT setval(
    pg_get_serial_sequence('maintenance_records', 'id'),
    COALESCE(MAX(id), 1),
    MAX(id) IS NOT NULL
) FROM maintenance_records;

SELECT setval(
    pg_get_serial_sequence('finance_transactions', 'id'),
    COALESCE(MAX(id), 1),
    MAX(id) IS NOT NULL
) FROM finance_transactions;

SELECT setval(
    pg_get_serial_sequence('trip_documents', 'id'),
    COALESCE(MAX(id), 1),
    MAX(id) IS NOT NULL
) FROM trip_documents;

SELECT setval(
    pg_get_serial_sequence('vehicle_documents', 'id'),
    COALESCE(MAX(id), 1),
    MAX(id) IS NOT NULL
) FROM vehicle_documents;
