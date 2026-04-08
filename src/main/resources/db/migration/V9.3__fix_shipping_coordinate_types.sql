-- V9.3: Fix coordinate types to match JPA Double (Double Precision)
-- and ensure consistency between checkout and order shipping addresses.

SET search_path TO commerce, public;

-- Fix order_shipping_addresses types
ALTER TABLE commerce.order_shipping_addresses
    ALTER COLUMN latitude TYPE double precision,
    ALTER COLUMN longitude TYPE double precision;

-- Add coordinates to checkout_shipping_addresses if missing (for parity)
ALTER TABLE commerce.checkout_shipping_addresses
    ADD COLUMN IF NOT EXISTS latitude double precision,
    ADD COLUMN IF NOT EXISTS longitude double precision;
