-- V2: Payment Audit and Idempotency Support
SET search_path TO public;

CREATE TABLE IF NOT EXISTS public.idempotency_records (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    request_hash VARCHAR(255) NOT NULL,
    response_payload JSONB,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(255);
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS fraud_status VARCHAR(50);
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS payment_expiry TIMESTAMP WITH TIME ZONE;
