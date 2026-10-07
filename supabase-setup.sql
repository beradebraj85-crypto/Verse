-- ==============================================================================
-- Supabase Database Initialization Script for Versē
-- Run this in your Supabase Dashboard -> SQL Editor
-- ==============================================================================

-- 1. Enable pgvector and UUID extensions
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Verify installed extensions
SELECT * FROM pg_extension WHERE extname IN ('vector', 'uuid-ossp');
