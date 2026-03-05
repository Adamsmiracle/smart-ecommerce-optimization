-- Add OAuth2 provider columns for Google login support
ALTER TABLE app_user ADD COLUMN oauth_provider VARCHAR(50);
ALTER TABLE app_user ADD COLUMN oauth_provider_id VARCHAR(255);

-- Allow nullable password_hash for OAuth2 users (they don't have a local password)
ALTER TABLE app_user ALTER COLUMN password_hash DROP NOT NULL;

-- Index for fast OAuth2 user lookup
CREATE INDEX idx_user_oauth ON app_user(oauth_provider, oauth_provider_id);


