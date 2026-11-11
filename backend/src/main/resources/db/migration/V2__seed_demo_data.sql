-- =============================================================================
-- LEDGERLINE SCHEMA MIGRATION V2: SEED DATA & SYSTEM CLEARING ACCOUNTS
-- =============================================================================

-- 1. SYSTEM CLEARING / RESERVE ACCOUNT (For Double-Entry Offsetting)
-- The system clearing account acts as the central counterparty for deposits and withdrawals,
-- ensuring that every single monetary movement preserves the double-entry accounting identity.
INSERT INTO accounts (account_number, user_id, account_type, currency, balance, status, created_at, updated_at)
VALUES ('ACC-SYSTEM-CLEARING', NULL, 'SYSTEM_CLEARING', 'INR', 0.0000, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
