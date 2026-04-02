-- V6.2: Recreate idempotency_records in commerce schema
SET search_path TO commerce, public;

CREATE TABLE IF NOT EXISTS commerce.idempotency_records (
    id UUID PRIMARY KEY,
    key VARCHAR(255) UNIQUE NOT NULL,
    request_hash VARCHAR(255),
    response_body JSONB,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE
);
