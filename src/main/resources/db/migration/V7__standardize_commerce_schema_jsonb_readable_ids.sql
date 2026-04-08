-- V7: Standardize commerce schema, implement JSONB payloads, and add readable order/trx IDs
SET search_path TO commerce, public;

-- 1. Orders table enhancements
ALTER TABLE commerce.orders
    ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0 NOT NULL,
    ADD COLUMN IF NOT EXISTS created_by UUID,
    ADD COLUMN IF NOT EXISTS updated_by UUID;

CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_order_number ON commerce.orders(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON commerce.orders(customer_id);

-- 2. Payments table enhancements (renaming transactions concept alignment)
-- We'll keep the table name 'payments' but add transaction_number for business readability
ALTER TABLE commerce.payments
    ADD COLUMN IF NOT EXISTS transaction_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0 NOT NULL,
    ADD COLUMN IF NOT EXISTS created_by UUID,
    ADD COLUMN IF NOT EXISTS updated_by UUID,
    ADD COLUMN IF NOT EXISTS provider_request_payload JSONB,
    ADD COLUMN IF NOT EXISTS provider_response_payload JSONB;

CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_transaction_number ON commerce.payments(transaction_number);
CREATE INDEX IF NOT EXISTS idx_payments_provider_order_id ON commerce.payments(provider_order_id);
CREATE INDEX IF NOT EXISTS idx_payments_provider_transaction_id ON commerce.payments(provider_transaction_id);

-- 3. Shipping Addresses Biteship-Ready Model
-- Check if columns exist before adding
DO $$
BEGIN
    -- We'll modify order_shipping_addresses to match Biteship requirements
    -- recipient_name maps to contact_name
    -- phone is already there
    -- line1 + line2 maps to address
    -- notes maps to note
    -- postal_code is already there
    -- area_id is already there

    -- Adding missing Biteship specific fields
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'commerce' AND table_name = 'order_shipping_addresses' AND column_name = 'latitude') THEN
        ALTER TABLE commerce.order_shipping_addresses ADD COLUMN latitude NUMERIC(10, 8);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'commerce' AND table_name = 'order_shipping_addresses' AND column_name = 'longitude') THEN
        ALTER TABLE commerce.order_shipping_addresses ADD COLUMN longitude NUMERIC(11, 8);
    END IF;
END $$;

-- 4. Webhook logs enhancement
CREATE TABLE IF NOT EXISTS commerce.webhook_logs (
    id UUID PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    provider_transaction_id VARCHAR(100),
    event_type VARCHAR(100),
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_webhook_logs_provider_trx ON commerce.webhook_logs(provider, provider_transaction_id);

-- 5. Audit fields for other core tables
DO $$
DECLARE
    t text;
BEGIN
    FOR t IN SELECT table_name FROM information_schema.tables
             WHERE table_schema = 'commerce'
               AND table_type = 'BASE TABLE'
               AND table_name IN ('order_items', 'order_shipping_addresses', 'order_shipping_selections')
    LOOP
        EXECUTE format('ALTER TABLE commerce.%I ADD COLUMN IF NOT EXISTS created_by UUID', t);
        EXECUTE format('ALTER TABLE commerce.%I ADD COLUMN IF NOT EXISTS updated_by UUID', t);
    END LOOP;
END $$;
