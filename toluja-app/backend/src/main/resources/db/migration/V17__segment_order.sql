ALTER TABLE segments
    ADD COLUMN ordem INTEGER;

WITH ranked_segments AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY nome ASC, id ASC) AS ordem_calculada
    FROM segments
)
UPDATE segments s
SET ordem = r.ordem_calculada
FROM ranked_segments r
WHERE s.id = r.id;

ALTER TABLE segments
    ALTER COLUMN ordem SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_segments_tenant_ordem ON segments(tenant_id, ordem);
CREATE UNIQUE INDEX IF NOT EXISTS uk_segments_tenant_ordem ON segments(tenant_id, ordem);
