-- V9.2: Ensure missing email columns are added to shipping address tables
-- This is a follow-up to V9.1 to ensure both columns are present if V9.1 was only partially applied.

SET search_path TO commerce, public;

ALTER TABLE commerce.checkout_shipping_addresses
ADD COLUMN IF NOT EXISTS email varchar(254);

ALTER TABLE commerce.order_shipping_addresses
ADD COLUMN IF NOT EXISTS email varchar(254);
