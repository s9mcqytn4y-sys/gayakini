-- V3: Shipment Tracking and Webhook Audit Alignment

CREATE TABLE IF NOT EXISTS shipments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    external_id VARCHAR(255), -- Biteship Order ID
    waybill_id VARCHAR(255), -- Tracking Number
    courier_company VARCHAR(100),
    courier_service VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    history JSONB,
    raw_payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS webhook_receipts (
    id UUID PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    external_id VARCHAR(255),
    event_type VARCHAR(100),
    payload JSONB NOT NULL,
    status VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT
);

CREATE INDEX idx_shipments_order ON shipments(order_id);
CREATE INDEX idx_shipments_waybill ON shipments(waybill_id);
CREATE INDEX idx_webhook_receipts_provider_external ON webhook_receipts(provider, external_id);
