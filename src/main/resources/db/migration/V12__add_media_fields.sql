-- Add media fields to Product, Customer, and Payment entities
ALTER TABLE commerce.products ADD COLUMN IF NOT EXISTS image_url TEXT;

ALTER TABLE commerce.customers ADD COLUMN IF NOT EXISTS profile_url TEXT;

ALTER TABLE commerce.payments ADD COLUMN IF NOT EXISTS proof_url TEXT;

COMMENT ON COLUMN commerce.products.image_url IS 'Relative path to the main product image';
COMMENT ON COLUMN commerce.customers.profile_url IS 'Relative path to the user profile picture';
COMMENT ON COLUMN commerce.payments.proof_url IS 'Relative path to the payment proof document or image';
