CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    nome_exibicao TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('ADMIN', 'ATENDENTE')),
    ativo INTEGER NOT NULL DEFAULT 1,
    deve_trocar_senha INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    preco NUMERIC NOT NULL,
    ativo INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,
    criado_em TEXT NOT NULL,
    status TEXT NOT NULL,
    total NUMERIC NOT NULL,
    observacao TEXT,
    user_id INTEGER NOT NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    nome_snapshot TEXT NOT NULL,
    preco_snapshot NUMERIC NOT NULL,
    quantidade INTEGER NOT NULL,
    subtotal NUMERIC NOT NULL,
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
