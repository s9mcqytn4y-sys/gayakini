-- V10: Centralized Audit Logging System
SET search_path TO commerce, public;

CREATE TABLE IF NOT EXISTS commerce.audit_logs (
    id UUID PRIMARY KEY,
    actor_id VARCHAR(255) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    previous_state JSONB,
    new_state JSONB,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON commerce.audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON commerce.audit_logs(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON commerce.audit_logs(created_at);
