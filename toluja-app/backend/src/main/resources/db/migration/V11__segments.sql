CREATE TABLE segments (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(80) NOT NULL,
    cor VARCHAR(7) NOT NULL,
    icone VARCHAR(40) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_segments_tenant_nome_lower ON segments(tenant_id, lower(nome));
CREATE INDEX IF NOT EXISTS idx_segments_tenant_nome ON segments(tenant_id, nome);

ALTER TABLE items ADD COLUMN segment_id INTEGER;
ALTER TABLE items
    ADD CONSTRAINT fk_items_segment
    FOREIGN KEY (segment_id) REFERENCES segments(id);

CREATE INDEX IF NOT EXISTS idx_items_tenant_segment ON items(tenant_id, segment_id);
