-- V16: Expand Inventory Adjustment Reasons
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
        'CANCELLATION_RESTOCK'
    ));
