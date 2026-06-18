ALTER TABLE email_verification_token ADD COLUMN token_id UUID DEFAULT gen_random_uuid();
ALTER TABLE password_reset_token ADD COLUMN token_id UUID DEFAULT gen_random_uuid();
ALTER TABLE user_session ADD COLUMN session_public_id UUID DEFAULT gen_random_uuid();

-- Add non-null constraints after generating IDs for existing rows
ALTER TABLE email_verification_token ALTER COLUMN token_id SET NOT NULL;
ALTER TABLE password_reset_token ALTER COLUMN token_id SET NOT NULL;
ALTER TABLE user_session ALTER COLUMN session_public_id SET NOT NULL;

-- Add unique indexes for O(1) lookups
CREATE UNIQUE INDEX uk_email_verification_token_id ON email_verification_token(token_id);
CREATE UNIQUE INDEX uk_password_reset_token_id ON password_reset_token(token_id);
CREATE UNIQUE INDEX uk_user_session_public_id ON user_session(session_public_id);
