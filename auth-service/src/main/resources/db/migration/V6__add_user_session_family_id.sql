-- Migration: Add family_id to user_session for refresh token reuse detection
-- Version: V6
-- Description: Adds family_id column to user_session table, backfills existing rows, and adds an index.

-- 1. Add the family_id column allowing NULL temporarily for backfilling
ALTER TABLE user_session ADD COLUMN family_id UUID;

-- 2. Backfill existing rows with a unique UUID for each row
-- This ensures that existing sessions are treated as separate families
UPDATE user_session SET family_id = gen_random_uuid() WHERE family_id IS NULL;

-- 3. Set family_id to NOT NULL
ALTER TABLE user_session ALTER COLUMN family_id SET NOT NULL;

-- 4. Create an index on family_id for efficient bulk revocation
CREATE INDEX idx_user_session_family_id ON user_session(family_id);
