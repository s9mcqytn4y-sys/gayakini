-- V2: Payment Audit and Idempotency Support

CREATE TABLE IF NOT EXISTS payment_webhooks (
    id UUID PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    external_id VARCHAR(255),
    event_type VARCHAR(100),
    payload JSON NOT NULL, -- Changed from JSONB for H2 compatibility
    status VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT
);

CREATE TABLE IF NOT EXISTS idempotency_records (
    id UUID PRIMARY KEY,
    "key" VARCHAR(255) UNIQUE NOT NULL, -- Quoted key for H2/Postgres compatibility
    request_hash VARCHAR(255),
    response_body JSON, -- Changed from JSONB for H2 compatibility
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_payment_webhooks_external ON payment_webhooks(external_id);
CREATE INDEX idx_idempotency_key ON idempotency_records("key");
