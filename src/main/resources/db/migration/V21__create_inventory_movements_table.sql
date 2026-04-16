-- V21: Create Inventory Movements Table
CREATE TABLE IF NOT EXISTS commerce.inventory_movements (
    id                   uuid PRIMARY KEY,
    variant_id           uuid NOT NULL REFERENCES commerce.product_variants(id) ON DELETE RESTRICT,
    quantity             integer NOT NULL,
    source_location      varchar(50) NOT NULL,
    destination_location varchar(50) NOT NULL,
    movement_type        varchar(30) NOT NULL CHECK (movement_type IN ('INTERNAL_TRANSFER', 'WAREHOUSE_PICKING', 'RESTOCKING', 'RETURNS')),
    reference_id         varchar(100),
    notes                varchar(500),
    created_at           timestamptz NOT NULL DEFAULT now()
);

-- Update inventory_adjustments reason_code check to include RETURN_RESTOCK_QC
ALTER TABLE commerce.inventory_adjustments DROP CONSTRAINT IF EXISTS inventory_adjustments_reason_code_check;
ALTER TABLE commerce.inventory_adjustments ADD CONSTRAINT inventory_adjustments_reason_code_check
    CHECK (reason_code IN (
        'INITIAL_STOCK',
        'MANUAL_CORRECTION',
        'DAMAGE',
        'LOST',
        'FOUND',
        'RETURN_RESTOCK',
        'RESERVATION',
        'RESERVATION_RELEASE',
        'SALE',
        'CANCELLATION_RESTOCK',
        'RETURN_RESTOCK_QC'
    ));
