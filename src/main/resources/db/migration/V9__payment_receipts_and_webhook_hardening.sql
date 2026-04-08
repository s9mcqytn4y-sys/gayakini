-- V9: Payment Receipts Audit and Webhook Hardening
SET search_path TO public;

CREATE TABLE IF NOT EXISTS public.payment_receipts (
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

CREATE INDEX IF NOT EXISTS idx_payment_receipts_provider_order_id ON public.payment_receipts(provider_order_id);
-- Unique index for idempotency: prevent processing the same status for the same order multiple times
CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_receipts_idempotency ON public.payment_receipts(provider_order_id, transaction_status) WHERE processing_status = 'PROCESSED';

-- Add unique constraint to avoid double processing if needed, but we'll handle it in application logic too
