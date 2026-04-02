-- V4.1: Create customer_addresses table (Missing in V4)
SET search_path TO commerce, public;

CREATE TABLE IF NOT EXISTS commerce.customer_addresses (
    id                   uuid PRIMARY KEY,
    customer_id          uuid NOT NULL REFERENCES commerce.customers(id) ON DELETE CASCADE,
    recipient_name       varchar(120) NOT NULL,
    phone                varchar(30) NOT NULL,
    line1                varchar(200) NOT NULL,
    line2                varchar(200),
    notes                varchar(200),
    area_id              varchar(100) NOT NULL REFERENCES commerce.shipping_areas(area_id),
    district             varchar(120) NOT NULL,
    city                 varchar(120) NOT NULL,
    province             varchar(120) NOT NULL,
    postal_code          varchar(20) NOT NULL,
    country_code         varchar(2) NOT NULL DEFAULT 'ID' CHECK (country_code = 'ID'),
    is_default           boolean NOT NULL DEFAULT false,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_customer_addresses_customer ON commerce.customer_addresses(customer_id);
