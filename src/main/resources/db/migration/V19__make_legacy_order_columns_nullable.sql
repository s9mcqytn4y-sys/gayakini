-- V19 Make legacy order columns nullable
-- These columns were renamed or replaced in V4 but kept the NOT NULL constraint from V1,
-- which prevents creating new orders as they are not mapped in the JPA entity.

ALTER TABLE commerce.orders ALTER COLUMN total_amount_legacy DROP NOT NULL;
ALTER TABLE commerce.orders ALTER COLUMN grand_total_legacy DROP NOT NULL;
