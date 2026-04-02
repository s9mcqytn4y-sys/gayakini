-- V3: Shipment Tracking and Webhook Audit Alignment
SET search_path TO public;

CREATE TABLE IF NOT EXISTS public.shipments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES public.orders(id),
    external_id VARCHAR(255),
    waybill_id VARCHAR(255),
    courier_company VARCHAR(100),
    courier_service VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    history JSONB,
    raw_payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.webhook_receipts (
    id UUID PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    external_id VARCHAR(255),
    event_type VARCHAR(100),
    payload JSONB NOT NULL,
    status VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_shipments_order ON public.shipments(order_id);
