-- V6: Final Schema Hardening for Brand Fashion E-Commerce PostgreSQL 18+
-- Ensuring 100% compliance with constraints, triggers, and indexes.

SET search_path TO commerce, public;

-- 1. Ensure commerce schema functions are attached to all tables that have updated_at
DO $$
DECLARE
    t text;
BEGIN
    -- Silence trigger notices to prevent log spam
    EXECUTE 'SET client_min_messages TO warning';

    FOR t IN SELECT table_name FROM information_schema.tables
             WHERE table_schema = 'commerce'
               AND table_type = 'BASE TABLE'
               AND table_name NOT LIKE 'v_%'
               AND table_name NOT IN ('webhook_receipts', 'idempotency_keys', 'flyway_schema_history')
    LOOP
        -- Only attach trigger if the table actually has an updated_at column
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'commerce' AND table_name = t AND column_name = 'updated_at') THEN
            EXECUTE format('DROP TRIGGER IF EXISTS trg_%I_touch_updated_at ON commerce.%I', t, t);
            EXECUTE format('CREATE TRIGGER trg_%I_touch_updated_at BEFORE UPDATE ON commerce.%I FOR EACH ROW EXECUTE FUNCTION commerce.touch_updated_at()', t, t);
        END IF;
    END LOOP;

    EXECUTE 'SET client_min_messages TO notice';
END $$;

-- 2. Add specific unique constraint for default merchant shipping origin
DO $$ BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'commerce' AND table_name = 'merchant_shipping_origins') THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uq_merchant_shipping_origins_default_active
            ON commerce.merchant_shipping_origins (is_default)
            WHERE is_default = true AND is_active = true;
    END IF;
END $$;

-- 3. Ensure search path and extensions
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;

-- 4. Add GIN index for product search if not already present
CREATE INDEX IF NOT EXISTS idx_products_search_trgm
    ON commerce.products USING gin (
        lower(title || ' ' || coalesce(subtitle, '') || ' ' || brand_name || ' ' || coalesce(description, '')) public.gin_trgm_ops
    );

-- 5. Fix any potential mismatch in order_shipping_selections
DO $$ BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'commerce' AND table_name = 'order_shipping_selections') THEN
        ALTER TABLE commerce.order_shipping_selections
            DROP CONSTRAINT IF EXISTS chk_order_shipping_selection_eta;
        ALTER TABLE commerce.order_shipping_selections
            ADD CONSTRAINT chk_order_shipping_selection_eta CHECK (
                estimated_days_min IS NULL OR estimated_days_max IS NULL OR estimated_days_max >= estimated_days_min
            );
    END IF;
END $$;

-- 6. Ensure default schema values for status
ALTER TABLE commerce.orders ALTER COLUMN status SET NOT NULL;
ALTER TABLE commerce.orders ALTER COLUMN payment_status SET DEFAULT 'PENDING';
ALTER TABLE commerce.orders ALTER COLUMN fulfillment_status SET DEFAULT 'UNFULFILLED';
