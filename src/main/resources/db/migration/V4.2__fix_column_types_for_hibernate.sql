-- V4.2: Fix column types to match Hibernate expectations
SET search_path TO commerce, public;

-- Hibernate expects varchar(3) for currency_code, but PostgreSQL char(3) returns bpchar
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'commerce' AND table_name = 'carts' AND column_name = 'currency_code') THEN
        ALTER TABLE commerce.carts ALTER COLUMN currency_code TYPE varchar(3);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'commerce' AND table_name = 'checkouts' AND column_name = 'currency_code') THEN
        ALTER TABLE commerce.checkouts ALTER COLUMN currency_code TYPE varchar(3);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'commerce' AND table_name = 'orders' AND column_name = 'currency_code') THEN
        ALTER TABLE commerce.orders ALTER COLUMN currency_code TYPE varchar(3);
    END IF;
END $$;

-- Also ensure country_code matches for existing tables
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'commerce' AND table_name = 'shipping_areas' AND column_name = 'country_code') THEN
        ALTER TABLE commerce.shipping_areas ALTER COLUMN country_code TYPE varchar(2);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'commerce' AND table_name = 'customer_addresses' AND column_name = 'country_code') THEN
        ALTER TABLE commerce.customer_addresses ALTER COLUMN country_code TYPE varchar(2);
    END IF;
END $$;

-- Fix smallint to integer for Hibernate mapping compatibility
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'commerce' AND table_name = 'checkout_shipping_quotes' AND column_name = 'estimated_days_min') THEN
        ALTER TABLE commerce.checkout_shipping_quotes ALTER COLUMN estimated_days_min TYPE integer;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'commerce' AND table_name = 'checkout_shipping_quotes' AND column_name = 'estimated_days_max') THEN
        ALTER TABLE commerce.checkout_shipping_quotes ALTER COLUMN estimated_days_max TYPE integer;
    END IF;
END $$;

-- Fix idempotency hash type
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'commerce' AND table_name = 'idempotency_keys' AND column_name = 'request_hash') THEN
        ALTER TABLE commerce.idempotency_keys ALTER COLUMN request_hash TYPE varchar(64);
    END IF;
END $$;
