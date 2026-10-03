-- =============================================================================
-- V4__add_embedding_to_products.sql
-- Adds a pgvector column to products for semantic-search embeddings.
--
-- Dimension 1536 matches OpenAI text-embedding-3-small output.
-- The column is nullable so existing rows are unaffected until their embeddings
-- are generated asynchronously by ProductEmbeddingService.
-- =============================================================================

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS embedding vector(1536);

-- IVFFlat index for approximate nearest-neighbour search (cosine similarity).
-- Build this index only after a meaningful number of rows have embeddings,
-- or Postgres will skip it entirely for tiny tables.
-- lists = sqrt(row_count) is a common starting heuristic.
CREATE INDEX IF NOT EXISTS idx_products_embedding_ivfflat
    ON products USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
