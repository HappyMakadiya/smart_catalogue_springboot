-- =============================================================================
-- V6__clear_embeddings_for_reindex.sql
-- Clears all existing embeddings so the startup backfill runner will process
-- them again. This is necessary because the embedding input format was updated
-- to include "search_document: " (required by the nomic-embed-text model).
-- =============================================================================

UPDATE products SET embedding = NULL;
