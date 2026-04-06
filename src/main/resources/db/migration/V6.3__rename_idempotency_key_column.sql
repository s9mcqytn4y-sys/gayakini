-- V6.3: Rename 'key' column to 'idempotency_key' to avoid reserved keyword conflicts in H2 and others.
SET search_path TO commerce, public;

ALTER TABLE commerce.idempotency_records RENAME COLUMN key TO idempotency_key;
