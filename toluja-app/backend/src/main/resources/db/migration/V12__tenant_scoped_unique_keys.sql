ALTER TABLE subitem_categories DROP CONSTRAINT IF EXISTS subitem_categories_nome_key;
DROP INDEX IF EXISTS uk_subitem_categories_tenant_nome_lower;
CREATE UNIQUE INDEX IF NOT EXISTS uk_subitem_categories_tenant_nome_lower
    ON subitem_categories (tenant_id, lower(nome));

DROP INDEX IF EXISTS uk_items_tenant_nome_ativo_lower;
CREATE UNIQUE INDEX IF NOT EXISTS uk_items_tenant_nome_ativo_lower
    ON items (tenant_id, lower(nome))
    WHERE ativo = 1;

ALTER TABLE subitems DROP CONSTRAINT IF EXISTS uk_subitem_category_nome;
DROP INDEX IF EXISTS uk_subitems_tenant_category_nome_ativo_lower;
CREATE UNIQUE INDEX IF NOT EXISTS uk_subitems_tenant_category_nome_ativo_lower
    ON subitems (tenant_id, category_id, lower(nome))
    WHERE ativo = 1;
