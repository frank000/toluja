DROP INDEX IF EXISTS uk_orders_data_senha_chamada;

CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_tenant_data_senha_chamada
ON orders (tenant_id, LEFT(criado_em, 10), senha_chamada);
