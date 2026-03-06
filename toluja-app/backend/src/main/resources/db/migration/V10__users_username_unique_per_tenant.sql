ALTER TABLE users DROP CONSTRAINT IF EXISTS users_username_key;

DROP INDEX IF EXISTS uk_users_tenant_username;

ALTER TABLE users
    ADD CONSTRAINT uk_users_tenant_username UNIQUE (tenant_id, username);
