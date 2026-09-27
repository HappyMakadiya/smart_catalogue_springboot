-- =============================================================================
-- V3__add_version_to_products.sql
-- Add optimistic locking version column to products table
-- =============================================================================

ALTER TABLE products
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

