-- =============================================================================
-- V5__resize_embedding_to_768.sql
-- Resizes the pgvector embedding column from 1536 dims (OpenAI) to 768 dims
-- (Ollama nomic-embed-text). All existing rows are NULL so no data is lost.
-- =============================================================================

-- Drop the old IVFFlat index (bound to the previous dimension count)
DROP INDEX IF EXISTS idx_products_embedding_ivfflat;

-- Re-create the column at the new dimension.
-- pgvector does not support ALTER COLUMN … TYPE between different vector
-- dimensions directly, so we drop and re-add.
ALTER TABLE products DROP COLUMN IF EXISTS embedding;
ALTER TABLE products ADD COLUMN embedding vector(768);

-- Rebuild the IVFFlat index for 768-dim cosine similarity search.
-- lists = 100 is a sensible default; tune to sqrt(row_count) once data grows.
CREATE INDEX IF NOT EXISTS idx_products_embedding_ivfflat
    ON products USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
