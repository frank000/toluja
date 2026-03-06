CREATE TABLE IF NOT EXISTS tenants (
    id SERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL UNIQUE,
    nome VARCHAR(120) NOT NULL,
    ativo INTEGER NOT NULL DEFAULT 1
);

INSERT INTO tenants (tenant_id, nome, ativo)
SELECT 'default', 'Tenant Padrão', 1
WHERE NOT EXISTS (SELECT 1 FROM tenants WHERE tenant_id = 'default');
