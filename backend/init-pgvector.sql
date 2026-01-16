-- ===========================================
-- AI Sales Brain - pgvector Initialization
-- ===========================================
-- This script runs automatically on first container startup

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Verify extension is installed
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
