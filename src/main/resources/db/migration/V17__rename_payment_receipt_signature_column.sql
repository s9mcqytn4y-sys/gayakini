-- V17: Rename signature_key_hash to received_signature in payment_receipts for JPA consistency

ALTER TABLE commerce.payment_receipts RENAME COLUMN signature_key_hash TO received_signature;
