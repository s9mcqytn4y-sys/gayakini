-- GAYAKINI LOCAL DEVELOPMENT SEED
-- This script adds an admin and a test customer for local development workflow.
-- Run this against your local 'gayakini' database.

SET search_path TO commerce;

-- 1. CLEANUP (Optional - Use with caution)
-- DELETE FROM customers WHERE email IN ('admin@gayakini.com', 'user@gayakini.com');

-- 2. SEED ADMIN (Password: password)
INSERT INTO customers (id, email, password_hash, full_name, role, is_active, created_at, updated_at)
VALUES (
    '018f0000-0000-7000-8000-000000000001',
    'admin@gayakini.com',
    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOnu',
    'Gayakini Admin',
    'ADMIN',
    true,
    now(),
    now()
) ON CONFLICT (email) DO NOTHING;

-- 3. SEED CUSTOMER (Password: password)
INSERT INTO customers (id, email, password_hash, full_name, role, is_active, created_at, updated_at)
VALUES (
    '018f0000-0000-7000-8000-000000000002',
    'user@gayakini.com',
    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOnu',
    'Gaya User',
    'CUSTOMER',
    true,
    now(),
    now()
) ON CONFLICT (email) DO NOTHING;

-- 4. SEED PRODUCTS (If needed for smoke tests)
-- Assuming products table exists as per V1 migration
INSERT INTO products (id, slug, title, subtitle, brand_name, description, status, created_at, updated_at)
VALUES (
    '018f0000-0000-7000-8000-000000000003',
    'gayakini-classic-tee',
    'Gayakini Classic Tee',
    'Essential Everyday Wear',
    'GAYAKINI',
    'High quality cotton tee for daily style.',
    'PUBLISHED',
    now(),
    now()
) ON CONFLICT (slug) DO NOTHING;
