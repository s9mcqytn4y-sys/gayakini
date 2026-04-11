-- V15: Create Promo Exclusions Table
SET search_path TO commerce, public;

CREATE TABLE commerce.promo_exclusions (
    id UUID PRIMARY KEY,
    promo_id UUID NOT NULL REFERENCES commerce.promos(id) ON DELETE CASCADE,
    exclusion_type VARCHAR(20) NOT NULL, -- CATEGORY, COLLECTION, PRODUCT
    excluded_entity_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_promo_exclusions_promo_id ON commerce.promo_exclusions(promo_id);
