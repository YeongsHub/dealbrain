-- ===========================================
-- AI Sales Brain - pgvector Initialization
-- ===========================================
-- This script runs automatically on first container startup

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create vector_store table for Spring AI PGVector
-- This table stores document embeddings for RAG queries
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding vector(1536)
);

-- Create HNSW index for fast similarity search
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
ON vector_store
USING hnsw (embedding vector_cosine_ops);

-- Verify extension is installed
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
