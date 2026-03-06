ALTER TABLE orders ADD COLUMN senha_chamada INTEGER;

WITH ordered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS senha
    FROM orders
)
UPDATE orders
SET senha_chamada = (
    SELECT ordered.senha
    FROM ordered
    WHERE ordered.id = orders.id
)
WHERE senha_chamada IS NULL;

ALTER TABLE orders ALTER COLUMN senha_chamada SET NOT NULL;

CREATE UNIQUE INDEX uk_orders_senha_chamada ON orders(senha_chamada);
