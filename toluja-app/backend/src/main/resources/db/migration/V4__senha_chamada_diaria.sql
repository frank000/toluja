DROP INDEX IF EXISTS uk_orders_senha_chamada;

CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_data_senha_chamada
ON orders (LEFT(criado_em, 10), senha_chamada);
