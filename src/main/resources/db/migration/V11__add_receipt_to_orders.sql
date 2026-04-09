-- Add receipt_path to orders table for invoice storage
ALTER TABLE commerce.orders ADD COLUMN IF NOT EXISTS receipt_path VARCHAR(255);

-- Audit log for migration
COMMENT ON COLUMN commerce.orders.receipt_path IS 'Relative path to the generated PDF invoice (YYYY/MM/DD/secure-name.pdf)';
