-- V18: Allow zero quantity_delta in inventory_adjustments to support reservation snapshots
ALTER TABLE commerce.inventory_adjustments DROP CONSTRAINT IF EXISTS inventory_adjustments_quantity_delta_check;
ALTER TABLE commerce.inventory_adjustments ADD CONSTRAINT inventory_adjustments_quantity_delta_check
    CHECK (quantity_delta BETWEEN -999999 AND 999999);
