ALTER TABLE users ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default';
ALTER TABLE items ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default';
ALTER TABLE orders ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default';
ALTER TABLE subitem_categories ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default';
ALTER TABLE subitems ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'default';

CREATE INDEX IF NOT EXISTS idx_users_tenant_username ON users(tenant_id, username);
CREATE INDEX IF NOT EXISTS idx_items_tenant_ativo ON items(tenant_id, ativo);
CREATE INDEX IF NOT EXISTS idx_orders_tenant_criado_em ON orders(tenant_id, criado_em);
CREATE INDEX IF NOT EXISTS idx_subitem_categories_tenant_nome ON subitem_categories(tenant_id, nome);
CREATE INDEX IF NOT EXISTS idx_subitems_tenant_ativo ON subitems(tenant_id, ativo);
