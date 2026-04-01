-- Brand Fashion E-Commerce PostgreSQL 18+ schema
-- Target runtime: Spring Boot + Flyway/Liquibase
-- Design principles:
-- 1) UUIDv7 primary keys for write locality and sortable IDs.
-- 2) Integer money amounts in IDR (rupiah) as bigint.
-- 3) Snapshot tables for cart/checkout/order to preserve business history.
-- 4) Hashed opaque guest tokens, never raw tokens.
-- 5) Explicit constraints, partial indexes, and provider audit tables.

CREATE SCHEMA IF NOT EXISTS commerce;
SET search_path TO commerce, public;

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;

-- -----------------------------------------------------------------------------
-- Utility functions
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION commerce.touch_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION commerce.sync_cart_totals()
RETURNS trigger
LANGUAGE plpgsql
AS $$
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

    UPDATE commerce.carts
       SET item_count = 0,
           subtotal_amount = 0,
           updated_at = now()
     WHERE id = v_cart_id
       AND NOT EXISTS (
            SELECT 1
              FROM commerce.cart_items ci
             WHERE ci.cart_id = v_cart_id
       );

    RETURN NULL;
END;
$$;

-- -----------------------------------------------------------------------------
-- Reference and master tables
-- -----------------------------------------------------------------------------
CREATE TABLE commerce.shipping_areas (
    area_id              varchar(100) PRIMARY KEY,
    provider             varchar(20) NOT NULL DEFAULT 'BITESHIP'
                         CHECK (provider IN ('BITESHIP')),
    label                varchar(250) NOT NULL,
    district             varchar(120),
    city                 varchar(120),
    province             varchar(120),
    postal_code          varchar(20),
    country_code         char(2) NOT NULL DEFAULT 'ID'
                         CHECK (country_code = 'ID'),
    raw_payload          jsonb,
    refreshed_at         timestamptz NOT NULL DEFAULT now(),
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_shipping_areas_touch_updated_at
BEFORE UPDATE ON commerce.shipping_areas
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

CREATE TABLE commerce.merchant_shipping_origins (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
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
    country_code         char(2) NOT NULL DEFAULT 'ID'
                         CHECK (country_code = 'ID'),
    latitude             numeric(10,7),
    longitude            numeric(10,7),
    is_default           boolean NOT NULL DEFAULT false,
    is_active            boolean NOT NULL DEFAULT true,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_merchant_shipping_origins_default_active
    ON commerce.merchant_shipping_origins (is_default)
    WHERE is_default = true AND is_active = true;

CREATE TRIGGER trg_merchant_shipping_origins_touch_updated_at
BEFORE UPDATE ON commerce.merchant_shipping_origins
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

CREATE TABLE commerce.categories (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    slug                 varchar(100) NOT NULL UNIQUE
                         CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
    name                 varchar(120) NOT NULL,
    description          text,
    is_active            boolean NOT NULL DEFAULT true,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_categories_touch_updated_at
BEFORE UPDATE ON commerce.categories
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

CREATE TABLE commerce.collections (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    slug                 varchar(100) NOT NULL UNIQUE
                         CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
    name                 varchar(120) NOT NULL,
    description          text,
    is_active            boolean NOT NULL DEFAULT true,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_collections_touch_updated_at
BEFORE UPDATE ON commerce.collections
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

-- -----------------------------------------------------------------------------
-- Identity and customer profile
-- -----------------------------------------------------------------------------
CREATE TABLE commerce.customers (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    email                varchar(254) NOT NULL,
    email_normalized     text GENERATED ALWAYS AS (lower(btrim(email))) STORED,
    phone                varchar(30),
    full_name            varchar(120) NOT NULL,
    password_hash        varchar(255) NOT NULL,
    role                 varchar(20) NOT NULL DEFAULT 'CUSTOMER'
                         CHECK (role IN ('CUSTOMER')),
    is_active            boolean NOT NULL DEFAULT true,
    last_login_at        timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_customers_full_name_not_blank CHECK (length(btrim(full_name)) > 0)
);

CREATE UNIQUE INDEX uq_customers_email_normalized
    ON commerce.customers (email_normalized);

CREATE TRIGGER trg_customers_touch_updated_at
BEFORE UPDATE ON commerce.customers
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

CREATE TABLE commerce.customer_refresh_tokens (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    customer_id          uuid NOT NULL REFERENCES commerce.customers(id) ON DELETE CASCADE,
    token_hash           char(64) NOT NULL UNIQUE,
    user_agent           varchar(500),
    issued_ip            inet,
    last_used_at         timestamptz,
    expires_at           timestamptz NOT NULL,
    revoked_at           timestamptz,
    replaced_by_token_id uuid REFERENCES commerce.customer_refresh_tokens(id),
    created_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_customer_refresh_tokens_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_customer_refresh_tokens_customer
    ON commerce.customer_refresh_tokens (customer_id, expires_at DESC);

CREATE TABLE commerce.customer_addresses (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    customer_id          uuid NOT NULL REFERENCES commerce.customers(id) ON DELETE CASCADE,
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
    country_code         char(2) NOT NULL DEFAULT 'ID'
                         CHECK (country_code = 'ID'),
    is_default           boolean NOT NULL DEFAULT false,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_customer_addresses_default_per_customer
    ON commerce.customer_addresses (customer_id)
    WHERE is_default = true;

CREATE INDEX idx_customer_addresses_customer
    ON commerce.customer_addresses (customer_id, is_default DESC, created_at DESC);

CREATE TRIGGER trg_customer_addresses_touch_updated_at
BEFORE UPDATE ON commerce.customer_addresses
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

-- -----------------------------------------------------------------------------
-- Catalog
-- -----------------------------------------------------------------------------
CREATE TABLE commerce.products (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    slug                 varchar(100) NOT NULL UNIQUE
                         CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
    title                varchar(180) NOT NULL,
    subtitle             varchar(180),
    brand_name           varchar(120) NOT NULL,
    category_id          uuid NOT NULL REFERENCES commerce.categories(id) ON DELETE RESTRICT,
    description          text NOT NULL,
    status               varchar(20) NOT NULL DEFAULT 'DRAFT'
                         CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    published_at         timestamptz,
    archived_at          timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_products_title_not_blank CHECK (length(btrim(title)) > 0),
    CONSTRAINT chk_products_brand_name_not_blank CHECK (length(btrim(brand_name)) > 0)
);

CREATE INDEX idx_products_catalog_browse
    ON commerce.products (status, category_id, created_at DESC)
    INCLUDE (slug, title, brand_name);

CREATE INDEX idx_products_search_trgm
    ON commerce.products USING gin (
        lower(title || ' ' || coalesce(subtitle, '') || ' ' || brand_name || ' ' || coalesce(description, '')) public.gin_trgm_ops
    );

CREATE TRIGGER trg_products_touch_updated_at
BEFORE UPDATE ON commerce.products
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

CREATE TABLE commerce.product_collections (
    product_id           uuid NOT NULL REFERENCES commerce.products(id) ON DELETE CASCADE,
    collection_id        uuid NOT NULL REFERENCES commerce.collections(id) ON DELETE RESTRICT,
    created_at           timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (product_id, collection_id)
);

CREATE INDEX idx_product_collections_collection
    ON commerce.product_collections (collection_id, product_id);

CREATE TABLE commerce.product_media (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    product_id           uuid NOT NULL REFERENCES commerce.products(id) ON DELETE CASCADE,
    url                  text NOT NULL,
    alt_text             varchar(200) NOT NULL,
    sort_order           integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_primary           boolean NOT NULL DEFAULT false,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_product_media_primary_per_product
    ON commerce.product_media (product_id)
    WHERE is_primary = true;

CREATE INDEX idx_product_media_product_sort
    ON commerce.product_media (product_id, sort_order ASC, created_at ASC);

CREATE TRIGGER trg_product_media_touch_updated_at
BEFORE UPDATE ON commerce.product_media
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

CREATE TABLE commerce.product_variants (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    product_id           uuid NOT NULL REFERENCES commerce.products(id) ON DELETE CASCADE,
    sku                  varchar(64) NOT NULL UNIQUE,
    status               varchar(20) NOT NULL DEFAULT 'ACTIVE'
                         CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    color                varchar(50) NOT NULL,
    size_code            varchar(20) NOT NULL,
    price_amount         bigint NOT NULL CHECK (price_amount >= 0),
    compare_at_amount    bigint CHECK (compare_at_amount IS NULL OR compare_at_amount >= price_amount),
    weight_grams         integer NOT NULL DEFAULT 0 CHECK (weight_grams >= 0),
    stock_on_hand        integer NOT NULL DEFAULT 0 CHECK (stock_on_hand >= 0),
    stock_reserved       integer NOT NULL DEFAULT 0 CHECK (stock_reserved >= 0),
    stock_available      integer GENERATED ALWAYS AS (GREATEST(stock_on_hand - stock_reserved, 0)) STORED,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_variants_product_option UNIQUE (product_id, color, size_code),
    CONSTRAINT chk_product_variants_reserved_not_exceed_on_hand CHECK (stock_reserved <= stock_on_hand)
);

CREATE INDEX idx_product_variants_public_lookup
    ON commerce.product_variants (product_id, status, color, size_code)
    INCLUDE (price_amount, compare_at_amount, weight_grams, stock_available);

CREATE INDEX idx_product_variants_stock_available
    ON commerce.product_variants (status, stock_available DESC, product_id);

CREATE TRIGGER trg_product_variants_touch_updated_at
BEFORE UPDATE ON commerce.product_variants
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

-- -----------------------------------------------------------------------------
-- Cart
-- -----------------------------------------------------------------------------
CREATE TABLE commerce.carts (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    customer_id          uuid REFERENCES commerce.customers(id) ON DELETE SET NULL,
    status               varchar(30) NOT NULL DEFAULT 'ACTIVE'
                         CHECK (status IN ('ACTIVE', 'CHECKOUT_IN_PROGRESS', 'CONVERTED', 'EXPIRED')),
    currency_code        char(3) NOT NULL DEFAULT 'IDR'
                         CHECK (currency_code = 'IDR'),
    access_token_hash    char(64) UNIQUE,
    expires_at           timestamptz,
    item_count           integer NOT NULL DEFAULT 0 CHECK (item_count >= 0),
    subtotal_amount      bigint NOT NULL DEFAULT 0 CHECK (subtotal_amount >= 0),
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_carts_owner_or_token CHECK (customer_id IS NOT NULL OR access_token_hash IS NOT NULL)
);

CREATE UNIQUE INDEX uq_carts_active_per_customer
    ON commerce.carts (customer_id)
    WHERE customer_id IS NOT NULL AND status = 'ACTIVE';

CREATE INDEX idx_carts_status_expiry
    ON commerce.carts (status, expires_at);

CREATE TRIGGER trg_carts_touch_updated_at
BEFORE UPDATE ON commerce.carts
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

CREATE TABLE commerce.cart_items (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    cart_id              uuid NOT NULL REFERENCES commerce.carts(id) ON DELETE CASCADE,
    product_id           uuid NOT NULL REFERENCES commerce.products(id) ON DELETE RESTRICT,
    variant_id           uuid NOT NULL REFERENCES commerce.product_variants(id) ON DELETE RESTRICT,
    product_title_snapshot varchar(180) NOT NULL,
    sku_snapshot         varchar(64) NOT NULL,
    color                varchar(50) NOT NULL,
    size_code            varchar(20) NOT NULL,
    quantity             integer NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    unit_price_amount    bigint NOT NULL CHECK (unit_price_amount >= 0),
    compare_at_amount    bigint CHECK (compare_at_amount IS NULL OR compare_at_amount >= unit_price_amount),
    primary_image_url    text,
    line_total_amount    bigint GENERATED ALWAYS AS (unit_price_amount * quantity::bigint) STORED,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_cart_items_variant_per_cart UNIQUE (cart_id, variant_id)
);

CREATE INDEX idx_cart_items_cart
    ON commerce.cart_items (cart_id, created_at ASC);

CREATE TRIGGER trg_cart_items_touch_updated_at
BEFORE UPDATE ON commerce.cart_items
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

CREATE TRIGGER trg_cart_items_sync_totals_ins
AFTER INSERT ON commerce.cart_items
FOR EACH ROW EXECUTE FUNCTION commerce.sync_cart_totals();

CREATE TRIGGER trg_cart_items_sync_totals_upd
AFTER UPDATE ON commerce.cart_items
FOR EACH ROW EXECUTE FUNCTION commerce.sync_cart_totals();

CREATE TRIGGER trg_cart_items_sync_totals_del
AFTER DELETE ON commerce.cart_items
FOR EACH ROW EXECUTE FUNCTION commerce.sync_cart_totals();

-- -----------------------------------------------------------------------------
-- Checkout
-- -----------------------------------------------------------------------------
CREATE TABLE commerce.checkouts (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    cart_id              uuid NOT NULL UNIQUE REFERENCES commerce.carts(id) ON DELETE RESTRICT,
    customer_id          uuid REFERENCES commerce.customers(id) ON DELETE SET NULL,
    status               varchar(30) NOT NULL DEFAULT 'ACTIVE'
                         CHECK (status IN ('ACTIVE', 'READY_FOR_ORDER', 'ORDER_CREATED', 'EXPIRED')),
    currency_code        char(3) NOT NULL DEFAULT 'IDR'
                         CHECK (currency_code = 'IDR'),
    access_token_hash    char(64) UNIQUE,
    subtotal_amount      bigint NOT NULL DEFAULT 0 CHECK (subtotal_amount >= 0),
    shipping_cost_amount bigint NOT NULL DEFAULT 0 CHECK (shipping_cost_amount >= 0),
    total_amount         bigint GENERATED ALWAYS AS (subtotal_amount + shipping_cost_amount) STORED,
    selected_shipping_quote_id uuid,
    expires_at           timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_checkouts_owner_or_token CHECK (customer_id IS NOT NULL OR access_token_hash IS NOT NULL)
);

CREATE INDEX idx_checkouts_status_expiry
    ON commerce.checkouts (status, expires_at);

CREATE TRIGGER trg_checkouts_touch_updated_at
BEFORE UPDATE ON commerce.checkouts
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

CREATE TABLE commerce.checkout_items (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    checkout_id          uuid NOT NULL REFERENCES commerce.checkouts(id) ON DELETE CASCADE,
    product_id           uuid NOT NULL REFERENCES commerce.products(id) ON DELETE RESTRICT,
    variant_id           uuid NOT NULL REFERENCES commerce.product_variants(id) ON DELETE RESTRICT,
    product_title_snapshot varchar(180) NOT NULL,
    sku_snapshot         varchar(64) NOT NULL,
    color                varchar(50) NOT NULL,
    size_code            varchar(20) NOT NULL,
    quantity             integer NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    unit_price_amount    bigint NOT NULL CHECK (unit_price_amount >= 0),
    compare_at_amount    bigint CHECK (compare_at_amount IS NULL OR compare_at_amount >= unit_price_amount),
    primary_image_url    text,
    line_total_amount    bigint GENERATED ALWAYS AS (unit_price_amount * quantity::bigint) STORED,
    created_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_checkout_items_variant_per_checkout UNIQUE (checkout_id, variant_id)
);

CREATE INDEX idx_checkout_items_checkout
    ON commerce.checkout_items (checkout_id, created_at ASC);

CREATE TABLE commerce.checkout_shipping_addresses (
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
    country_code         char(2) NOT NULL DEFAULT 'ID'
                         CHECK (country_code = 'ID'),
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_checkout_shipping_addresses_touch_updated_at
BEFORE UPDATE ON commerce.checkout_shipping_addresses
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

CREATE TABLE commerce.checkout_shipping_quotes (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    checkout_id          uuid NOT NULL REFERENCES commerce.checkouts(id) ON DELETE CASCADE,
    provider             varchar(20) NOT NULL DEFAULT 'BITESHIP'
                         CHECK (provider IN ('BITESHIP')),
    provider_reference   varchar(120),
    courier_code         varchar(50) NOT NULL,
    courier_name         varchar(120) NOT NULL,
    service_code         varchar(50) NOT NULL,
    service_name         varchar(120) NOT NULL,
    description          varchar(200),
    cost_amount          bigint NOT NULL CHECK (cost_amount >= 0),
    estimated_days_min   smallint CHECK (estimated_days_min IS NULL OR estimated_days_min >= 1),
    estimated_days_max   smallint CHECK (estimated_days_max IS NULL OR estimated_days_max >= 1),
    is_recommended       boolean NOT NULL DEFAULT false,
    raw_payload          jsonb,
    expires_at           timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_checkout_shipping_quotes_eta CHECK (
        estimated_days_min IS NULL OR estimated_days_max IS NULL OR estimated_days_max >= estimated_days_min
    ),
    CONSTRAINT uq_checkout_shipping_quotes_checkout_id_id UNIQUE (checkout_id, id)
);

CREATE INDEX idx_checkout_shipping_quotes_checkout
    ON commerce.checkout_shipping_quotes (checkout_id, created_at DESC)
    INCLUDE (courier_code, service_code, cost_amount, expires_at);

CREATE TRIGGER trg_checkout_shipping_quotes_touch_updated_at
BEFORE UPDATE ON commerce.checkout_shipping_quotes
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

ALTER TABLE commerce.checkouts
    ADD CONSTRAINT fk_checkouts_selected_shipping_quote
    FOREIGN KEY (id, selected_shipping_quote_id)
    REFERENCES commerce.checkout_shipping_quotes (checkout_id, id)
    ON DELETE SET NULL;

-- -----------------------------------------------------------------------------
-- Orders
-- -----------------------------------------------------------------------------
CREATE TABLE commerce.orders (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    order_number         varchar(50) NOT NULL UNIQUE,
    checkout_id          uuid NOT NULL UNIQUE REFERENCES commerce.checkouts(id) ON DELETE RESTRICT,
    cart_id              uuid NOT NULL UNIQUE REFERENCES commerce.carts(id) ON DELETE RESTRICT,
    customer_id          uuid REFERENCES commerce.customers(id) ON DELETE SET NULL,
    access_token_hash    char(64) UNIQUE,
    status               varchar(30) NOT NULL
                         CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'READY_TO_SHIP', 'SHIPPED', 'COMPLETED', 'CANCELLED')),
    payment_status       varchar(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'EXPIRED', 'CANCELLED', 'REFUNDED')),
    fulfillment_status   varchar(20) NOT NULL DEFAULT 'UNFULFILLED'
                         CHECK (fulfillment_status IN ('UNFULFILLED', 'BOOKED', 'IN_TRANSIT', 'DELIVERED', 'RETURNED', 'CANCELLED')),
    currency_code        char(3) NOT NULL DEFAULT 'IDR'
                         CHECK (currency_code = 'IDR'),
    subtotal_amount      bigint NOT NULL CHECK (subtotal_amount >= 0),
    shipping_cost_amount bigint NOT NULL DEFAULT 0 CHECK (shipping_cost_amount >= 0),
    total_amount         bigint GENERATED ALWAYS AS (subtotal_amount + shipping_cost_amount) STORED,
    current_payment_id   uuid,
    customer_notes       varchar(500),
    placed_at            timestamptz NOT NULL DEFAULT now(),
    paid_at              timestamptz,
    cancelled_at         timestamptz,
    cancellation_reason  varchar(300),
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_orders_owner_or_token CHECK (customer_id IS NOT NULL OR access_token_hash IS NOT NULL)
);

CREATE INDEX idx_orders_customer_created
    ON commerce.orders (customer_id, created_at DESC)
    INCLUDE (order_number, status, payment_status, fulfillment_status, total_amount);

CREATE INDEX idx_orders_admin_filters
    ON commerce.orders (status, payment_status, fulfillment_status, created_at DESC)
    INCLUDE (order_number, customer_id, total_amount);

CREATE TRIGGER trg_orders_touch_updated_at
BEFORE UPDATE ON commerce.orders
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

CREATE TABLE commerce.order_items (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
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

CREATE INDEX idx_order_items_order
    ON commerce.order_items (order_id, created_at ASC);

CREATE TABLE commerce.order_shipping_addresses (
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
    country_code         char(2) NOT NULL DEFAULT 'ID'
                         CHECK (country_code = 'ID'),
    created_at           timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE commerce.order_shipping_selections (
    order_id             uuid PRIMARY KEY REFERENCES commerce.orders(id) ON DELETE CASCADE,
    provider             varchar(20) NOT NULL DEFAULT 'BITESHIP'
                         CHECK (provider IN ('BITESHIP')),
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
    created_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_order_shipping_selection_eta CHECK (
        estimated_days_min IS NULL OR estimated_days_max IS NULL OR estimated_days_max >= estimated_days_min
    )
);

-- -----------------------------------------------------------------------------
-- Payment and shipping integrations
-- -----------------------------------------------------------------------------
CREATE TABLE commerce.payments (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    order_id             uuid NOT NULL REFERENCES commerce.orders(id) ON DELETE RESTRICT,
    provider             varchar(20) NOT NULL DEFAULT 'MIDTRANS'
                         CHECK (provider IN ('MIDTRANS')),
    flow                 varchar(20) NOT NULL DEFAULT 'SNAP'
                         CHECK (flow IN ('SNAP')),
    status               varchar(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'EXPIRED', 'CANCELLED', 'REFUNDED')),
    preferred_channel    varchar(30)
                         CHECK (preferred_channel IS NULL OR preferred_channel IN ('AUTO', 'CREDIT_CARD', 'BANK_TRANSFER', 'GOPAY', 'SHOPEEPAY', 'QRIS', 'CSTORE')),
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
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_payments_order_id_id UNIQUE (order_id, id)
);

CREATE UNIQUE INDEX uq_payments_provider_transaction_id
    ON commerce.payments (provider_transaction_id)
    WHERE provider_transaction_id IS NOT NULL;

CREATE UNIQUE INDEX uq_payments_pending_per_order
    ON commerce.payments (order_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_payments_order_created
    ON commerce.payments (order_id, created_at DESC)
    INCLUDE (status, expires_at, provider_transaction_id);

CREATE TRIGGER trg_payments_touch_updated_at
BEFORE UPDATE ON commerce.payments
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

ALTER TABLE commerce.orders
    ADD CONSTRAINT fk_orders_current_payment
    FOREIGN KEY (id, current_payment_id)
    REFERENCES commerce.payments (order_id, id)
    ON DELETE SET NULL;

CREATE TABLE commerce.shipments (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    order_id             uuid NOT NULL UNIQUE REFERENCES commerce.orders(id) ON DELETE RESTRICT,
    provider             varchar(20) NOT NULL DEFAULT 'BITESHIP'
                         CHECK (provider IN ('BITESHIP')),
    provider_order_id    varchar(100),
    provider_draft_order_id varchar(100),
    provider_reference_id varchar(100),
    status               varchar(20) NOT NULL
                         CHECK (status IN ('UNFULFILLED', 'BOOKED', 'IN_TRANSIT', 'DELIVERED', 'RETURNED', 'CANCELLED')),
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

CREATE UNIQUE INDEX uq_shipments_provider_order_id
    ON commerce.shipments (provider_order_id)
    WHERE provider_order_id IS NOT NULL;

CREATE UNIQUE INDEX uq_shipments_provider_draft_order_id
    ON commerce.shipments (provider_draft_order_id)
    WHERE provider_draft_order_id IS NOT NULL;

CREATE INDEX idx_shipments_status
    ON commerce.shipments (status, created_at DESC)
    INCLUDE (order_id, tracking_number);

CREATE TRIGGER trg_shipments_touch_updated_at
BEFORE UPDATE ON commerce.shipments
FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at();

-- -----------------------------------------------------------------------------
-- Inventory control
-- -----------------------------------------------------------------------------
CREATE TABLE commerce.inventory_adjustments (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    variant_id           uuid NOT NULL REFERENCES commerce.product_variants(id) ON DELETE RESTRICT,
    quantity_delta       integer NOT NULL CHECK (quantity_delta BETWEEN -999999 AND 999999 AND quantity_delta <> 0),
    reason_code          varchar(30) NOT NULL
                         CHECK (reason_code IN ('INITIAL_STOCK', 'MANUAL_CORRECTION', 'DAMAGE', 'LOST', 'FOUND', 'RETURN_RESTOCK')),
    note                 varchar(500),
    actor_subject        varchar(150),
    idempotency_key      varchar(128),
    stock_on_hand_after  integer NOT NULL CHECK (stock_on_hand_after >= 0),
    stock_reserved_after integer NOT NULL CHECK (stock_reserved_after >= 0),
    created_at           timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_inventory_adjustments_idempotency_key
    ON commerce.inventory_adjustments (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_inventory_adjustments_variant_created
    ON commerce.inventory_adjustments (variant_id, created_at DESC);

CREATE TABLE commerce.inventory_reservations (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    order_id             uuid NOT NULL REFERENCES commerce.orders(id) ON DELETE RESTRICT,
    order_item_id        uuid NOT NULL UNIQUE REFERENCES commerce.order_items(id) ON DELETE RESTRICT,
    variant_id           uuid NOT NULL REFERENCES commerce.product_variants(id) ON DELETE RESTRICT,
    quantity             integer NOT NULL CHECK (quantity >= 1),
    status               varchar(20) NOT NULL DEFAULT 'ACTIVE'
                         CHECK (status IN ('ACTIVE', 'RELEASED', 'CONSUMED')),
    reserved_at          timestamptz NOT NULL DEFAULT now(),
    released_at          timestamptz,
    consumed_at          timestamptz,
    release_reason       varchar(100),
    CONSTRAINT uq_inventory_reservations_order_variant UNIQUE (order_id, variant_id)
);

CREATE INDEX idx_inventory_reservations_active_variant
    ON commerce.inventory_reservations (variant_id, reserved_at ASC)
    INCLUDE (order_id, quantity)
    WHERE status = 'ACTIVE';

-- -----------------------------------------------------------------------------
-- Idempotency and provider audit
-- -----------------------------------------------------------------------------
CREATE TABLE commerce.idempotency_keys (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    scope                varchar(80) NOT NULL,
    idempotency_key      varchar(128) NOT NULL,
    requester_type       varchar(30) NOT NULL,
    requester_id         uuid,
    request_hash         char(64) NOT NULL,
    resource_type        varchar(50),
    resource_id          uuid,
    response_status      integer CHECK (response_status BETWEEN 100 AND 599),
    response_body        jsonb,
    created_at           timestamptz NOT NULL DEFAULT now(),
    expires_at           timestamptz NOT NULL,
    CONSTRAINT uq_idempotency_scope_key UNIQUE (scope, idempotency_key),
    CONSTRAINT chk_idempotency_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_idempotency_expiry
    ON commerce.idempotency_keys (expires_at);

CREATE TABLE commerce.webhook_receipts (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    provider             varchar(20) NOT NULL
                         CHECK (provider IN ('MIDTRANS', 'BITESHIP')),
    event_name           varchar(100),
    dedup_key            char(64) NOT NULL UNIQUE,
    signature_valid      boolean,
    headers_jsonb        jsonb NOT NULL DEFAULT '{}'::jsonb,
    payload_jsonb        jsonb NOT NULL,
    processing_status    varchar(20) NOT NULL DEFAULT 'RECEIVED'
                         CHECK (processing_status IN ('RECEIVED', 'PROCESSED', 'IGNORED', 'FAILED')),
    received_at          timestamptz NOT NULL DEFAULT now(),
    processed_at         timestamptz,
    error_message        text
);

CREATE INDEX idx_webhook_receipts_provider_received
    ON commerce.webhook_receipts (provider, received_at DESC)
    INCLUDE (processing_status, event_name);

CREATE TABLE commerce.payment_status_histories (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    payment_id           uuid NOT NULL REFERENCES commerce.payments(id) ON DELETE CASCADE,
    order_id             uuid NOT NULL REFERENCES commerce.orders(id) ON DELETE RESTRICT,
    source               varchar(20) NOT NULL
                         CHECK (source IN ('WEBHOOK', 'STATUS_API', 'API', 'MANUAL')),
    provider_order_id    varchar(50),
    provider_transaction_id varchar(100),
    provider_transaction_status varchar(50),
    provider_fraud_status varchar(50),
    provider_status_code varchar(10),
    normalized_payment_status varchar(20) NOT NULL
                         CHECK (normalized_payment_status IN ('PENDING', 'PAID', 'FAILED', 'EXPIRED', 'CANCELLED', 'REFUNDED')),
    gross_amount         bigint CHECK (gross_amount IS NULL OR gross_amount >= 0),
    raw_payload          jsonb,
    created_at           timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_status_histories_payment_created
    ON commerce.payment_status_histories (payment_id, created_at DESC);

CREATE TABLE commerce.shipment_status_histories (
    id                   uuid PRIMARY KEY DEFAULT uuidv7(),
    shipment_id          uuid NOT NULL REFERENCES commerce.shipments(id) ON DELETE CASCADE,
    order_id             uuid NOT NULL REFERENCES commerce.orders(id) ON DELETE RESTRICT,
    source               varchar(20) NOT NULL
                         CHECK (source IN ('WEBHOOK', 'API', 'MANUAL')),
    event_name           varchar(100),
    provider_status      varchar(50),
    normalized_fulfillment_status varchar(20) NOT NULL
                         CHECK (normalized_fulfillment_status IN ('UNFULFILLED', 'BOOKED', 'IN_TRANSIT', 'DELIVERED', 'RETURNED', 'CANCELLED')),
    courier_tracking_id  varchar(100),
    courier_waybill_id   varchar(100),
    shipping_fee_amount  bigint CHECK (shipping_fee_amount IS NULL OR shipping_fee_amount >= 0),
    raw_payload          jsonb,
    created_at           timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_shipment_status_histories_shipment_created
    ON commerce.shipment_status_histories (shipment_id, created_at DESC);

-- -----------------------------------------------------------------------------
-- Read model view for storefront summary
-- -----------------------------------------------------------------------------
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
JOIN commerce.categories c
  ON c.id = p.category_id
LEFT JOIN LATERAL (
    SELECT m.url
      FROM commerce.product_media m
     WHERE m.product_id = p.id
     ORDER BY m.is_primary DESC, m.sort_order ASC, m.created_at ASC
     LIMIT 1
) pm ON true
LEFT JOIN commerce.product_variants v
  ON v.product_id = p.id
WHERE p.status = 'PUBLISHED'
GROUP BY p.id, p.slug, p.title, p.subtitle, p.brand_name, c.slug, pm.url, p.created_at;

-- -----------------------------------------------------------------------------
-- Suggested transactional rules for the service layer (reference only)
-- -----------------------------------------------------------------------------
-- 1. Place order:
--    - SELECT ... FOR UPDATE product_variants rows for all ordered variants.
--    - Validate stock_available >= requested quantity.
--    - Create orders, order_items, order_shipping_* snapshots.
--    - Insert inventory_reservations.
--    - Increment product_variants.stock_reserved.
--    - Mark checkout ORDER_CREATED and cart CONVERTED atomically.
--
-- 2. Payment success:
--    - Verify Midtrans signature.
--    - Reconcile with GET Status API.
--    - Update payments + payment_status_histories.
--    - Update orders.payment_status/status/current_payment_id/paid_at.
--
-- 3. Shipment booking:
--    - Use order_shipping_* snapshot + default merchant_shipping_origin.
--    - Create shipments row and shipment_status_histories.
--
-- 4. Cancel / expire order:
--    - Release ACTIVE inventory_reservations.
--    - Decrement product_variants.stock_reserved.
--    - Update orders.status/payment_status/fulfillment_status.
