-- V9.1: Fix Schema Mismatch for BootRun and Hibernate
-- The current Hibernate entities expect 'email' column in 'checkout_shipping_addresses'
-- and 'order_shipping_addresses' but it was missing from V5 migration.

SET search_path TO commerce, public;

ALTER TABLE commerce.checkout_shipping_addresses
ADD COLUMN IF NOT EXISTS email varchar(254);

ALTER TABLE commerce.order_shipping_addresses
ADD COLUMN IF NOT EXISTS email varchar(254);
