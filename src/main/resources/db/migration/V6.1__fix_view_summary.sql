-- V6.1: Fix v_public_product_summaries view to include missing columns required by JPA entity
-- Target: commerce schema

SET search_path TO commerce, public;

DROP VIEW IF EXISTS commerce.v_public_product_summaries;

CREATE OR REPLACE VIEW commerce.v_public_product_summaries AS
SELECT
    p.id,
    p.slug,
    p.title,
    p.subtitle,
    p.brand_name,
    c.slug AS category_slug,
    (SELECT string_agg(coll.slug, ',')
     FROM commerce.product_collections pc
     JOIN commerce.collections coll ON coll.id = pc.collection_id
     WHERE pc.product_id = p.id) AS collection_slug,
    (SELECT string_agg(DISTINCT v.color, ',')
     FROM commerce.product_variants v
     WHERE v.product_id = p.id) AS color,
    (SELECT string_agg(DISTINCT v.size_code, ',')
     FROM commerce.product_variants v
     WHERE v.product_id = p.id) AS size_code,
    pm.url AS primary_image_url,
    (SELECT MIN(v.price_amount) FROM commerce.product_variants v WHERE v.product_id = p.id AND v.status = 'ACTIVE') AS min_price_amount,
    (SELECT MAX(v.price_amount) FROM commerce.product_variants v WHERE v.product_id = p.id AND v.status = 'ACTIVE') AS max_price_amount,
    COALESCE((SELECT BOOL_OR(v.stock_available > 0 AND v.status = 'ACTIVE') FROM commerce.product_variants v WHERE v.product_id = p.id), false) AS in_stock,
    p.created_at
FROM commerce.products p
JOIN commerce.categories c ON c.id = p.category_id
LEFT JOIN LATERAL (
    SELECT m.url FROM commerce.product_media m
     WHERE m.product_id = p.id
     ORDER BY m.is_primary DESC, m.sort_order ASC, m.created_at ASC
     LIMIT 1
) pm ON true
WHERE p.status = 'PUBLISHED';
