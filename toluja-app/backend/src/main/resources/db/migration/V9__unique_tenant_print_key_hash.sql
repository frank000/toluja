DROP INDEX IF EXISTS idx_tenants_print_key_hash;
CREATE UNIQUE INDEX IF NOT EXISTS uk_tenants_print_key_hash ON tenants(print_key_hash);
