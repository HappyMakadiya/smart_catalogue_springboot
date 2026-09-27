-- =============================================================================
-- V1__init_schema.sql
-- Initial schema: enable pgvector extension, create users, categories, products
-- =============================================================================

-- Enable pgvector so we can store embedding columns later
CREATE EXTENSION IF NOT EXISTS vector;

-- -----------------------------------------------------------------------------
-- users
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id       BIGSERIAL    PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(50)  NOT NULL DEFAULT 'ROLE_USER'
);

-- -----------------------------------------------------------------------------
-- categories
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- products
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(255)    NOT NULL,
    description TEXT,
    price       NUMERIC(19, 2)  NOT NULL CHECK (price >= 0),
    stock       INTEGER         NOT NULL DEFAULT 0 CHECK (stock >= 0),
    category_id BIGINT          REFERENCES categories (id) ON DELETE SET NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Index on category_id for fast joins / filtering
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products (category_id);
