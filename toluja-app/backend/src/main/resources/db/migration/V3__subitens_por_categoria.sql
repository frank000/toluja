CREATE TABLE subitem_categories (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE subitems (
    id SERIAL PRIMARY KEY,
    category_id INTEGER NOT NULL,
    nome VARCHAR(80) NOT NULL,
    preco NUMERIC(10, 2) NOT NULL,
    ativo INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_subitems_category FOREIGN KEY (category_id) REFERENCES subitem_categories(id),
    CONSTRAINT uk_subitem_category_nome UNIQUE (category_id, nome)
);

CREATE TABLE item_subitem_categories (
    item_id INTEGER NOT NULL,
    category_id INTEGER NOT NULL,
    PRIMARY KEY (item_id, category_id),
    CONSTRAINT fk_item_subitem_categories_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_item_subitem_categories_category FOREIGN KEY (category_id) REFERENCES subitem_categories(id)
);

CREATE TABLE order_item_subitems (
    id SERIAL PRIMARY KEY,
    order_item_id INTEGER NOT NULL,
    subitem_id INTEGER NOT NULL,
    categoria_nome_snapshot VARCHAR(80) NOT NULL,
    nome_snapshot VARCHAR(80) NOT NULL,
    preco_snapshot NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_order_item_subitems_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    CONSTRAINT fk_order_item_subitems_subitem FOREIGN KEY (subitem_id) REFERENCES subitems(id)
);
