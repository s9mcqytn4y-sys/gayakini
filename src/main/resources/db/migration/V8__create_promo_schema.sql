-- V8: Create Promo Engine Schema
SET search_path TO commerce, public;

CREATE TABLE commerce.promos (
    id UUID PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(20) NOT NULL, -- FIXED_AMOUNT, PERCENTAGE
    value NUMERIC(19, 4) NOT NULL,
    max_discount_amount NUMERIC(19, 4),
    min_order_value NUMERIC(19, 4) DEFAULT 0 NOT NULL,
    usage_limit INTEGER,
    current_usage INTEGER DEFAULT 0 NOT NULL,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    version INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_promos_code ON commerce.promos(code);
CREATE INDEX idx_promos_active_dates ON commerce.promos(is_active, start_date, end_date);

ALTER TABLE commerce.checkouts
    ADD COLUMN promo_code VARCHAR(50),
    ADD COLUMN discount_amount BIGINT DEFAULT 0 NOT NULL;

ALTER TABLE commerce.orders
    ADD COLUMN promo_code VARCHAR(50),
    ADD COLUMN discount_amount BIGINT DEFAULT 0 NOT NULL;
