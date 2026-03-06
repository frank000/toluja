CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    nome_exibicao VARCHAR(120) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'ATENDENTE')),
    ativo INTEGER NOT NULL DEFAULT 1,
    deve_trocar_senha INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE items (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    preco NUMERIC(10, 2) NOT NULL,
    ativo INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(5) NOT NULL UNIQUE,
    criado_em VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total NUMERIC(12, 2) NOT NULL,
    observacao VARCHAR(300),
    user_id INTEGER NOT NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    nome_snapshot VARCHAR(255) NOT NULL,
    preco_snapshot NUMERIC(10, 2) NOT NULL,
    quantidade INTEGER NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_item FOREIGN KEY (item_id) REFERENCES items(id)
);

INSERT INTO users (username, password_hash, nome_exibicao, role, ativo, deve_trocar_senha)
VALUES (
    'admin',
    '$2b$12$hj81EftB3HdhyYXa6GA50uJzZHtyO6fREA/EQdn4tse7KFnIPYBWu',
    'Administrador',
    'ADMIN',
    1,
    1
);
