ALTER TABLE tenants ADD COLUMN print_key_hash VARCHAR(64);

UPDATE tenants
SET print_key_hash = '75c8afea8878192aaa29cb72be13cb3a0552c8b52154b395eb739cafeff24e60'
WHERE print_key_hash IS NULL OR trim(print_key_hash) = '';

ALTER TABLE tenants ALTER COLUMN print_key_hash SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_tenants_print_key_hash ON tenants(print_key_hash);
