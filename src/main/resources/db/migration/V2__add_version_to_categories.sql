-- =============================================================================
-- V2__add_version_to_categories.sql
-- Add optimistic locking version column to categories table
-- =============================================================================

ALTER TABLE categories
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
