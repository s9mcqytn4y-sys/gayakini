-- V4: Aligning to PostgreSQL 18+ Final Schema
-- This migration moves existing tables to 'commerce' schema and refactors them to match the source of truth.

CREATE SCHEMA IF NOT EXISTS commerce;

-- Set search_path for this session
SET search_path TO commerce, public;

-- 1. Create shipping_areas (New Master Table)
CREATE TABLE IF NOT EXISTS commerce.shipping_areas (
    area_id              varchar(100) PRIMARY KEY,
    provider             varchar(20) NOT NULL DEFAULT 'BITESHIP' CHECK (provider IN ('BITESHIP')),
    label                varchar(250) NOT NULL,
    district             varchar(120),
    city                 varchar(120),
    province             varchar(120),
    postal_code          varchar(20),
    country_code         varchar(2) NOT NULL DEFAULT 'ID' CHECK (country_code = 'ID'),
    raw_payload          jsonb,
    refreshed_at         timestamp with time zone NOT NULL DEFAULT now(),
    created_at           timestamp with time zone NOT NULL DEFAULT now(),
    updated_at           timestamp with time zone NOT NULL DEFAULT now()
);

-- 2. Refactor and move 'customers'
DO $$ BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'customers') THEN
        ALTER TABLE public.customers SET SCHEMA commerce;
    END IF;
END $$;

ALTER TABLE commerce.customers RENAME COLUMN phone_number TO phone;
ALTER TABLE commerce.customers ADD COLUMN IF NOT EXISTS email_normalized text GENERATED ALWAYS AS (lower(btrim(email))) STORED;
ALTER TABLE commerce.customers ADD COLUMN IF NOT EXISTS role varchar(20) NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER'));
ALTER TABLE commerce.customers ADD COLUMN IF NOT EXISTS is_active boolean NOT NULL DEFAULT true;
ALTER TABLE commerce.customers ADD COLUMN IF NOT EXISTS last_login_at timestamp with time zone;
CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_email_normalized ON commerce.customers (email_normalized);

-- 3. Categories and Collections
CREATE TABLE IF NOT EXISTS commerce.categories (
    id                   uuid PRIMARY KEY,
    slug                 varchar(100) NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
    name                 varchar(120) NOT NULL,
    description          text,
    is_active            boolean NOT NULL DEFAULT true,
    created_at           timestamp with time zone NOT NULL DEFAULT now(),
    updated_at           timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS commerce.collections (
    id                   uuid PRIMARY KEY,
    slug                 varchar(100) NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$'),
    name                 varchar(120) NOT NULL,
    description          text,
    is_active            boolean NOT NULL DEFAULT true,
    created_at           timestamp with time zone NOT NULL DEFAULT now(),
    updated_at           timestamp with time zone NOT NULL DEFAULT now()
);

-- 4. Refactor and move 'products'
DO $$ BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'products') THEN
        ALTER TABLE public.products SET SCHEMA commerce;
    END IF;
END $$;

ALTER TABLE commerce.products RENAME COLUMN name TO title;
ALTER TABLE commerce.products ADD COLUMN IF NOT EXISTS subtitle varchar(180);
ALTER TABLE commerce.products ADD COLUMN IF NOT EXISTS brand_name varchar(120) NOT NULL DEFAULT 'GAYAKINI';
ALTER TABLE commerce.products ADD COLUMN IF NOT EXISTS category_id uuid REFERENCES commerce.categories(id);
ALTER TABLE commerce.products ADD COLUMN IF NOT EXISTS status varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'));
ALTER TABLE commerce.products ADD COLUMN IF NOT EXISTS published_at timestamp with time zone;
ALTER TABLE commerce.products ADD COLUMN IF NOT EXISTS archived_at timestamp with time zone;
ALTER TABLE commerce.products DROP COLUMN IF EXISTS base_price; -- Price is at variant level now

-- 5. Refactor and move 'product_variants'
DO $$ BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'product_variants') THEN
        ALTER TABLE public.product_variants SET SCHEMA commerce;
    END IF;
END $$;

ALTER TABLE commerce.product_variants RENAME COLUMN name TO size_code; -- Assuming name was used for size
ALTER TABLE commerce.product_variants ADD COLUMN IF NOT EXISTS color varchar(50) NOT NULL DEFAULT 'Default';
ALTER TABLE commerce.product_variants RENAME COLUMN price TO price_amount;
ALTER TABLE commerce.product_variants RENAME COLUMN stock TO stock_on_hand;
ALTER TABLE commerce.product_variants ADD COLUMN IF NOT EXISTS stock_reserved integer NOT NULL DEFAULT 0;
ALTER TABLE commerce.product_variants ADD COLUMN IF NOT EXISTS stock_available integer GENERATED ALWAYS AS (GREATEST(stock_on_hand - stock_reserved, 0)) STORED;
ALTER TABLE commerce.product_variants ADD COLUMN IF NOT EXISTS status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'));
ALTER TABLE commerce.product_variants ADD COLUMN IF NOT EXISTS compare_at_amount bigint;

-- 6. Refactor and move 'carts' and 'cart_items'
DO $$ BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'carts') THEN
        ALTER TABLE public.carts SET SCHEMA commerce;
    END IF;
END $$;

ALTER TABLE commerce.carts RENAME COLUMN guest_token_hash TO access_token_hash;
ALTER TABLE commerce.carts ADD COLUMN IF NOT EXISTS status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CHECKOUT_IN_PROGRESS', 'CONVERTED', 'EXPIRED'));
ALTER TABLE commerce.carts ADD COLUMN IF NOT EXISTS currency_code varchar(3) NOT NULL DEFAULT 'IDR';
ALTER TABLE commerce.carts ADD COLUMN IF NOT EXISTS expires_at timestamp with time zone;
ALTER TABLE commerce.carts ADD COLUMN IF NOT EXISTS item_count integer NOT NULL DEFAULT 0;
ALTER TABLE commerce.carts ADD COLUMN IF NOT EXISTS subtotal_amount bigint NOT NULL DEFAULT 0;

DO $$ BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'cart_items') THEN
        ALTER TABLE public.cart_items SET SCHEMA commerce;
    END IF;
END $$;

ALTER TABLE commerce.cart_items ADD COLUMN IF NOT EXISTS product_id uuid REFERENCES commerce.products(id);
ALTER TABLE commerce.cart_items ADD COLUMN IF NOT EXISTS product_title_snapshot varchar(180);
ALTER TABLE commerce.cart_items ADD COLUMN IF NOT EXISTS sku_snapshot varchar(64);
ALTER TABLE commerce.cart_items ADD COLUMN IF NOT EXISTS color varchar(50);
ALTER TABLE commerce.cart_items ADD COLUMN IF NOT EXISTS size_code varchar(20);

DO $$ BEGIN
    IF EXISTS (SELECT column_name FROM information_schema.columns WHERE table_schema = 'commerce' AND table_name = 'cart_items' AND column_name = 'quantity') THEN
        ALTER TABLE commerce.cart_items RENAME COLUMN quantity TO quantity_legacy;
        ALTER TABLE commerce.cart_items ADD COLUMN quantity integer CHECK (quantity BETWEEN 1 AND 99);
        UPDATE commerce.cart_items SET quantity = quantity_legacy;
        ALTER TABLE commerce.cart_items DROP COLUMN quantity_legacy;
    END IF;
END $$;

ALTER TABLE commerce.cart_items ADD COLUMN IF NOT EXISTS unit_price_amount bigint NOT NULL DEFAULT 0;
ALTER TABLE commerce.cart_items ADD COLUMN IF NOT EXISTS compare_at_amount bigint;
ALTER TABLE commerce.cart_items ADD COLUMN IF NOT EXISTS primary_image_url text;
ALTER TABLE commerce.cart_items ADD COLUMN IF NOT EXISTS line_total_amount bigint GENERATED ALWAYS AS (unit_price_amount * quantity::bigint) STORED;

-- 7. Checkout Tables (New based on schema)
CREATE TABLE IF NOT EXISTS commerce.checkouts (
    id                   uuid PRIMARY KEY,
    cart_id              uuid NOT NULL UNIQUE REFERENCES commerce.carts(id),
    customer_id          uuid REFERENCES commerce.customers(id),
    status               varchar(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'READY_FOR_ORDER', 'ORDER_CREATED', 'EXPIRED')),
    currency_code        varchar(3) NOT NULL DEFAULT 'IDR',
    access_token_hash    varchar(64) UNIQUE,
    subtotal_amount      bigint NOT NULL DEFAULT 0,
    shipping_cost_amount bigint NOT NULL DEFAULT 0,
    total_amount         bigint GENERATED ALWAYS AS (subtotal_amount + shipping_cost_amount) STORED,
    selected_shipping_quote_id uuid,
    expires_at           timestamp with time zone,
    created_at           timestamp with time zone NOT NULL DEFAULT now(),
    updated_at           timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS commerce.checkout_items (
    id                   uuid PRIMARY KEY,
    checkout_id          uuid NOT NULL REFERENCES commerce.checkouts(id) ON DELETE CASCADE,
    product_id           uuid NOT NULL REFERENCES commerce.products(id),
    variant_id           uuid NOT NULL REFERENCES commerce.product_variants(id),
    product_title_snapshot varchar(180) NOT NULL,
    sku_snapshot         varchar(64) NOT NULL,
    color                varchar(50) NOT NULL,
    size_code            varchar(20) NOT NULL,
    quantity             integer NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    unit_price_amount    bigint NOT NULL,
    compare_at_amount    bigint,
    primary_image_url    text,
    line_total_amount    bigint GENERATED ALWAYS AS (unit_price_amount * quantity::bigint) STORED,
    created_at           timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS commerce.checkout_shipping_quotes (
    id                   uuid PRIMARY KEY,
    checkout_id          uuid NOT NULL REFERENCES commerce.checkouts(id) ON DELETE CASCADE,
    provider             varchar(20) NOT NULL DEFAULT 'BITESHIP',
    provider_reference   varchar(120),
    courier_code         varchar(50) NOT NULL,
    courier_name         varchar(120) NOT NULL,
    service_code         varchar(50) NOT NULL,
    service_name         varchar(120) NOT NULL,
    description          varchar(200),
    cost_amount          bigint NOT NULL,
    estimated_days_min   smallint,
    estimated_days_max   smallint,
    is_recommended       boolean NOT NULL DEFAULT false,
    raw_payload          jsonb,
    expires_at           timestamp with time zone,
    created_at           timestamp with time zone NOT NULL DEFAULT now(),
    updated_at           timestamp with time zone NOT NULL DEFAULT now()
);

-- 8. Refactor 'orders'
DO $$ BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'orders') THEN
        ALTER TABLE public.orders SET SCHEMA commerce;
    END IF;
END $$;

ALTER TABLE commerce.orders RENAME COLUMN guest_token_hash TO access_token_hash;
ALTER TABLE commerce.orders ADD COLUMN IF NOT EXISTS checkout_id uuid REFERENCES commerce.checkouts(id);
ALTER TABLE commerce.orders ADD COLUMN IF NOT EXISTS cart_id uuid REFERENCES commerce.carts(id);
ALTER TABLE commerce.orders ADD COLUMN IF NOT EXISTS payment_status varchar(20) NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'EXPIRED', 'CANCELLED', 'REFUNDED'));
ALTER TABLE commerce.orders ADD COLUMN IF NOT EXISTS fulfillment_status varchar(20) NOT NULL DEFAULT 'UNFULFILLED' CHECK (fulfillment_status IN ('UNFULFILLED', 'BOOKED', 'IN_TRANSIT', 'DELIVERED', 'RETURNED', 'CANCELLED'));
ALTER TABLE commerce.orders ADD COLUMN IF NOT EXISTS currency_code varchar(3) NOT NULL DEFAULT 'IDR';
ALTER TABLE commerce.orders RENAME COLUMN total_amount TO total_amount_legacy;
ALTER TABLE commerce.orders ADD COLUMN IF NOT EXISTS subtotal_amount bigint NOT NULL DEFAULT 0;
ALTER TABLE commerce.orders RENAME COLUMN shipping_cost TO shipping_cost_amount;
ALTER TABLE commerce.orders ADD COLUMN IF NOT EXISTS total_amount bigint GENERATED ALWAYS AS (subtotal_amount + shipping_cost_amount) STORED;
ALTER TABLE commerce.orders ADD COLUMN IF NOT EXISTS current_payment_id uuid;
ALTER TABLE commerce.orders RENAME COLUMN grand_total TO grand_total_legacy;
ALTER TABLE commerce.orders ADD COLUMN IF NOT EXISTS customer_notes varchar(500);
ALTER TABLE commerce.orders ADD COLUMN IF NOT EXISTS placed_at timestamp with time zone NOT NULL DEFAULT now();
ALTER TABLE commerce.orders ADD COLUMN IF NOT EXISTS paid_at timestamp with time zone;
ALTER TABLE commerce.orders ADD COLUMN IF NOT EXISTS cancelled_at timestamp with time zone;
ALTER TABLE commerce.orders ADD COLUMN IF NOT EXISTS cancellation_reason varchar(300);

-- 9. Inventory Control
CREATE TABLE IF NOT EXISTS commerce.inventory_reservations (
    id                   uuid PRIMARY KEY,
    order_id             uuid NOT NULL REFERENCES commerce.orders(id),
    order_item_id        uuid NOT NULL UNIQUE, -- References order_items.id
    variant_id           uuid NOT NULL REFERENCES commerce.product_variants(id),
    quantity             integer NOT NULL CHECK (quantity >= 1),
    status               varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'RELEASED', 'CONSUMED')),
    reserved_at          timestamp with time zone NOT NULL DEFAULT now(),
    released_at          timestamp with time zone,
    consumed_at          timestamp with time zone,
    release_reason       varchar(100)
);

-- 10. Idempotency Keys (Final structure)
CREATE TABLE IF NOT EXISTS commerce.idempotency_keys (
    id                   uuid PRIMARY KEY,
    scope                varchar(80) NOT NULL,
    idempotency_key      varchar(128) NOT NULL,
    requester_type       varchar(30) NOT NULL,
    requester_id         uuid,
    request_hash         char(64) NOT NULL,
    resource_type        varchar(50),
    resource_id          uuid,
    response_status      integer CHECK (response_status BETWEEN 100 AND 599),
    response_body        jsonb,
    created_at           timestamp with time zone NOT NULL DEFAULT now(),
    expires_at           timestamp with time zone NOT NULL,
    CONSTRAINT uq_idempotency_scope_key UNIQUE (scope, idempotency_key),
    CONSTRAINT chk_idempotency_expiry CHECK (expires_at > created_at)
);

-- Drop legacy idempotency table
DROP TABLE IF EXISTS public.idempotency_records;
