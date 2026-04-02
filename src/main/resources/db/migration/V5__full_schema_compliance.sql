-- V5: Full Schema Compliance with Brand Fashion E-Commerce PostgreSQL 18+ Contract
-- Target: commerce schema

SET search_path TO commerce, public;

-- 1. Utility Functions & Triggers
CREATE OR REPLACE FUNCTION commerce.touch_updated_at()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION commerce.sync_cart_totals()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_cart_id uuid;
BEGIN
    v_cart_id := COALESCE(NEW.cart_id, OLD.cart_id);
    UPDATE commerce.carts c
       SET item_count = COALESCE(s.item_count, 0),
           subtotal_amount = COALESCE(s.subtotal_amount, 0),
           updated_at = now()
      FROM (
            SELECT ci.cart_id,
                   COUNT(*)::integer AS item_count,
                   COALESCE(SUM(ci.line_total_amount), 0)::bigint AS subtotal_amount
              FROM commerce.cart_items ci
             WHERE ci.cart_id = v_cart_id
             GROUP BY ci.cart_id
           ) s
     WHERE c.id = v_cart_id;
    RETURN NULL;
END;
$$;

-- 2. Reference & Master Missing Tables
CREATE TABLE IF NOT EXISTS commerce.merchant_shipping_origins (
    id                   uuid PRIMARY KEY,
    code                 varchar(50) NOT NULL UNIQUE,
    name                 varchar(120) NOT NULL,
    contact_name         varchar(120) NOT NULL,
    contact_phone        varchar(30) NOT NULL,
    contact_email        varchar(254),
    line1                varchar(200) NOT NULL,
    line2                varchar(200),
    notes                varchar(200),
    area_id              varchar(100) REFERENCES commerce.shipping_areas(area_id),
    district             varchar(120) NOT NULL,
    city                 varchar(120) NOT NULL,
    province             varchar(120) NOT NULL,
    postal_code          varchar(20) NOT NULL,
    country_code         char(2) NOT NULL DEFAULT 'ID' CHECK (country_code = 'ID'),
    latitude             numeric(10,7),
    longitude            numeric(10,7),
    is_default           boolean NOT NULL DEFAULT false,
    is_active            boolean NOT NULL DEFAULT true,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

-- 3. Catalog Enhancements
CREATE TABLE IF NOT EXISTS commerce.product_collections (
    product_id           uuid NOT NULL REFERENCES commerce.products(id) ON DELETE CASCADE,
    collection_id        uuid NOT NULL REFERENCES commerce.collections(id) ON DELETE RESTRICT,
    created_at           timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (product_id, collection_id)
);

CREATE TABLE IF NOT EXISTS commerce.product_media (
    id                   uuid PRIMARY KEY,
    product_id           uuid NOT NULL REFERENCES commerce.products(id) ON DELETE CASCADE,
    url                  text NOT NULL,
    alt_text             varchar(200) NOT NULL,
    sort_order           integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_primary           boolean NOT NULL DEFAULT false,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

-- 4. Cart & Checkout Missing Structures
CREATE TABLE IF NOT EXISTS commerce.checkout_shipping_addresses (
    checkout_id          uuid PRIMARY KEY REFERENCES commerce.checkouts(id) ON DELETE CASCADE,
    customer_address_id  uuid REFERENCES commerce.customer_addresses(id) ON DELETE SET NULL,
    recipient_name       varchar(120) NOT NULL,
    phone                varchar(30) NOT NULL,
    line1                varchar(200) NOT NULL,
    line2                varchar(200),
    notes                varchar(200),
    area_id              varchar(100) NOT NULL REFERENCES commerce.shipping_areas(area_id),
    district             varchar(120) NOT NULL,
    city                 varchar(120) NOT NULL,
    province             varchar(120) NOT NULL,
    postal_code          varchar(20) NOT NULL,
    country_code         char(2) NOT NULL DEFAULT 'ID' CHECK (country_code = 'ID'),
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

-- 5. Order Missing Snapshots
CREATE TABLE IF NOT EXISTS commerce.order_items (
    id                   uuid PRIMARY KEY,
    order_id             uuid NOT NULL REFERENCES commerce.orders(id) ON DELETE CASCADE,
    product_id           uuid NOT NULL REFERENCES commerce.products(id) ON DELETE RESTRICT,
    variant_id           uuid NOT NULL REFERENCES commerce.product_variants(id) ON DELETE RESTRICT,
    sku_snapshot         varchar(64) NOT NULL,
    title_snapshot       varchar(180) NOT NULL,
    color                varchar(50) NOT NULL,
    size_code            varchar(20) NOT NULL,
    quantity             integer NOT NULL CHECK (quantity >= 1),
    unit_price_amount    bigint NOT NULL CHECK (unit_price_amount >= 0),
    line_total_amount    bigint GENERATED ALWAYS AS (unit_price_amount * quantity::bigint) STORED,
    created_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_order_items_variant_per_order UNIQUE (order_id, variant_id)
);

CREATE TABLE IF NOT EXISTS commerce.order_shipping_addresses (
    order_id             uuid PRIMARY KEY REFERENCES commerce.orders(id) ON DELETE CASCADE,
    source_address_id    uuid REFERENCES commerce.customer_addresses(id) ON DELETE SET NULL,
    recipient_name       varchar(120) NOT NULL,
    phone                varchar(30) NOT NULL,
    line1                varchar(200) NOT NULL,
    line2                varchar(200),
    notes                varchar(200),
    area_id              varchar(100) NOT NULL,
    district             varchar(120) NOT NULL,
    city                 varchar(120) NOT NULL,
    province             varchar(120) NOT NULL,
    postal_code          varchar(20) NOT NULL,
    country_code         char(2) NOT NULL DEFAULT 'ID' CHECK (country_code = 'ID'),
    created_at           timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS commerce.order_shipping_selections (
    order_id             uuid PRIMARY KEY REFERENCES commerce.orders(id) ON DELETE CASCADE,
    provider             varchar(20) NOT NULL DEFAULT 'BITESHIP' CHECK (provider IN ('BITESHIP')),
    provider_reference   varchar(120),
    courier_code         varchar(50) NOT NULL,
    courier_name         varchar(120) NOT NULL,
    service_code         varchar(50) NOT NULL,
    service_name         varchar(120) NOT NULL,
    description          varchar(200),
    cost_amount          bigint NOT NULL CHECK (cost_amount >= 0),
    estimated_days_min   smallint CHECK (estimated_days_min IS NULL OR estimated_days_min >= 1),
    estimated_days_max   smallint CHECK (estimated_days_max IS NULL OR estimated_days_max >= 1),
    raw_quote_payload    jsonb,
    created_at           timestamptz NOT NULL DEFAULT now()
);

-- 6. Payment & Shipment Refactoring
-- Drop legacy public tables if they still exist or were not properly moved
DROP TABLE IF EXISTS public.payments CASCADE;
DROP TABLE IF EXISTS public.shipments CASCADE;

CREATE TABLE IF NOT EXISTS commerce.payments (
    id                   uuid PRIMARY KEY,
    order_id             uuid NOT NULL REFERENCES commerce.orders(id) ON DELETE RESTRICT,
    provider             varchar(20) NOT NULL DEFAULT 'MIDTRANS' CHECK (provider IN ('MIDTRANS')),
    flow                 varchar(20) NOT NULL DEFAULT 'SNAP' CHECK (flow IN ('SNAP')),
    status               varchar(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'EXPIRED', 'CANCELLED', 'REFUNDED')),
    preferred_channel    varchar(30),
    enabled_channels     jsonb,
    provider_order_id    varchar(50) NOT NULL UNIQUE,
    provider_transaction_id varchar(100),
    gross_amount         bigint NOT NULL CHECK (gross_amount >= 0),
    raw_provider_status  varchar(100),
    snap_token           text,
    snap_redirect_url    text,
    provider_request_payload  jsonb,
    provider_response_payload jsonb,
    expires_at           timestamptz,
    paid_at              timestamptz,
    failed_at            timestamptz,
    cancelled_at         timestamptz,
    expired_at           timestamptz,
    refunded_at          timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS commerce.shipments (
    id                   uuid PRIMARY KEY,
    order_id             uuid NOT NULL UNIQUE REFERENCES commerce.orders(id) ON DELETE RESTRICT,
    provider             varchar(20) NOT NULL DEFAULT 'BITESHIP' CHECK (provider IN ('BITESHIP')),
    provider_order_id    varchar(100),
    provider_draft_order_id varchar(100),
    provider_reference_id varchar(100),
    status               varchar(20) NOT NULL CHECK (status IN ('UNFULFILLED', 'BOOKED', 'IN_TRANSIT', 'DELIVERED', 'RETURNED', 'CANCELLED')),
    raw_provider_status  varchar(100),
    courier_code         varchar(50),
    courier_name         varchar(120),
    service_code         varchar(50),
    service_name         varchar(120),
    tracking_number      varchar(100),
    tracking_url         text,
    shipping_fee_amount  bigint CHECK (shipping_fee_amount IS NULL OR shipping_fee_amount >= 0),
    note                 varchar(300),
    provider_request_payload  jsonb,
    provider_response_payload jsonb,
    booked_at            timestamptz,
    shipped_at           timestamptz,
    delivered_at         timestamptz,
    returned_at          timestamptz,
    cancelled_at         timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

-- 7. Inventory Adjustments
CREATE TABLE IF NOT EXISTS commerce.inventory_adjustments (
    id                   uuid PRIMARY KEY,
    variant_id           uuid NOT NULL REFERENCES commerce.product_variants(id) ON DELETE RESTRICT,
    quantity_delta       integer NOT NULL CHECK (quantity_delta BETWEEN -999999 AND 999999 AND quantity_delta <> 0),
    reason_code          varchar(30) NOT NULL CHECK (reason_code IN ('INITIAL_STOCK', 'MANUAL_CORRECTION', 'DAMAGE', 'LOST', 'FOUND', 'RETURN_RESTOCK')),
    note                 varchar(500),
    actor_subject        varchar(150),
    idempotency_key      varchar(128),
    stock_on_hand_after  integer NOT NULL CHECK (stock_on_hand_after >= 0),
    stock_reserved_after integer NOT NULL CHECK (stock_reserved_after >= 0),
    created_at           timestamptz NOT NULL DEFAULT now()
);

-- 8. Webhook Receipts & History
CREATE TABLE IF NOT EXISTS commerce.webhook_receipts (
    id                   uuid PRIMARY KEY,
    provider             varchar(20) NOT NULL CHECK (provider IN ('MIDTRANS', 'BITESHIP')),
    event_name           varchar(100),
    dedup_key            char(64) NOT NULL UNIQUE,
    signature_valid      boolean,
    headers_jsonb        jsonb NOT NULL DEFAULT '{}'::jsonb,
    payload_jsonb        jsonb NOT NULL,
    processing_status    varchar(20) NOT NULL DEFAULT 'RECEIVED' CHECK (processing_status IN ('RECEIVED', 'PROCESSED', 'IGNORED', 'FAILED')),
    received_at          timestamptz NOT NULL DEFAULT now(),
    processed_at         timestamptz,
    error_message        text
);

CREATE TABLE IF NOT EXISTS commerce.payment_status_histories (
    id                   uuid PRIMARY KEY,
    payment_id           uuid NOT NULL REFERENCES commerce.payments(id) ON DELETE CASCADE,
    order_id             uuid NOT NULL REFERENCES commerce.orders(id) ON DELETE RESTRICT,
    source               varchar(20) NOT NULL CHECK (source IN ('WEBHOOK', 'STATUS_API', 'API', 'MANUAL')),
    provider_order_id    varchar(50),
    provider_transaction_id varchar(100),
    provider_transaction_status varchar(50),
    provider_fraud_status varchar(50),
    provider_status_code varchar(10),
    normalized_payment_status varchar(20) NOT NULL CHECK (normalized_payment_status IN ('PENDING', 'PAID', 'FAILED', 'EXPIRED', 'CANCELLED', 'REFUNDED')),
    gross_amount         bigint CHECK (gross_amount IS NULL OR gross_amount >= 0),
    raw_payload          jsonb,
    created_at           timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS commerce.shipment_status_histories (
    id                   uuid PRIMARY KEY,
    shipment_id          uuid NOT NULL REFERENCES commerce.shipments(id) ON DELETE CASCADE,
    order_id             uuid NOT NULL REFERENCES commerce.orders(id) ON DELETE RESTRICT,
    source               varchar(20) NOT NULL CHECK (source IN ('WEBHOOK', 'API', 'MANUAL')),
    event_name           varchar(100),
    provider_status      varchar(50),
    normalized_fulfillment_status varchar(20) NOT NULL CHECK (normalized_fulfillment_status IN ('UNFULFILLED', 'BOOKED', 'IN_TRANSIT', 'DELIVERED', 'RETURNED', 'CANCELLED')),
    courier_tracking_id  varchar(100),
    courier_waybill_id   varchar(100),
    shipping_fee_amount  bigint CHECK (shipping_fee_amount IS NULL OR shipping_fee_amount >= 0),
    raw_payload          jsonb,
    created_at           timestamptz NOT NULL DEFAULT now()
);

-- 9. Read Models
CREATE OR REPLACE VIEW commerce.v_public_product_summaries AS
SELECT
    p.id,
    p.slug,
    p.title,
    p.subtitle,
    p.brand_name,
    c.slug AS category_slug,
    pm.url AS primary_image_url,
    MIN(v.price_amount) FILTER (WHERE v.status = 'ACTIVE') AS min_price_amount,
    MAX(v.price_amount) FILTER (WHERE v.status = 'ACTIVE') AS max_price_amount,
    COALESCE(BOOL_OR(v.stock_available > 0 AND v.status = 'ACTIVE'), false) AS in_stock,
    p.created_at
FROM commerce.products p
JOIN commerce.categories c ON c.id = p.category_id
LEFT JOIN LATERAL (
    SELECT m.url FROM commerce.product_media m
     WHERE m.product_id = p.id
     ORDER BY m.is_primary DESC, m.sort_order ASC, m.created_at ASC
     LIMIT 1
) pm ON true
LEFT JOIN commerce.product_variants v ON v.product_id = p.id
WHERE p.status = 'PUBLISHED'
GROUP BY p.id, p.slug, p.title, p.subtitle, p.brand_name, c.slug, pm.url, p.created_at;

-- 10. Triggers for Auditing
CREATE TRIGGER trg_cart_items_sync_totals
AFTER INSERT OR UPDATE OR DELETE ON commerce.cart_items
FOR EACH ROW EXECUTE FUNCTION commerce.sync_cart_totals();
