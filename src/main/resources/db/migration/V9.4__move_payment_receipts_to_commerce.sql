-- V9.4: Move payment_receipts to commerce schema for consistency with JPA/Hibernate configuration

SET search_path TO commerce, public;

-- 1. Create the table in commerce schema (matching V9 definition)
CREATE TABLE IF NOT EXISTS commerce.payment_receipts (
    id UUID PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    provider_order_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255),
    transaction_status VARCHAR(100) NOT NULL,
    fraud_status VARCHAR(50),
    signature_key_hash VARCHAR(255) NOT NULL,
    raw_payload JSONB NOT NULL,
    processing_status VARCHAR(50) NOT NULL, -- PENDING, PROCESSED, FAILED, SKIPPED
    received_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT
);

-- 2. Migrate any data if it exists in public (though unlikely on dev machines)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'payment_receipts') THEN
        INSERT INTO commerce.payment_receipts SELECT * FROM public.payment_receipts ON CONFLICT DO NOTHING;
        DROP TABLE public.payment_receipts CASCADE;
    END IF;
END $$;

-- 3. Ensure indexes are present in the correct schema
CREATE INDEX IF NOT EXISTS idx_payment_receipts_provider_order_id ON commerce.payment_receipts(provider_order_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_receipts_idempotency ON commerce.payment_receipts(provider_order_id, transaction_status) WHERE processing_status = 'PROCESSED';
