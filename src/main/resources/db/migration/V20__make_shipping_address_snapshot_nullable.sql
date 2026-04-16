-- V20 Make shipping_address_snapshot nullable
-- This column was added in V4 with a NOT NULL constraint,
-- which prevents creating new orders as it is not currently mapped in the JPA entity.

ALTER TABLE commerce.orders ALTER COLUMN shipping_address_snapshot DROP NOT NULL;
