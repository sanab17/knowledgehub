-- V3__Create_Trigram_Search_Indexes.sql
-- Enable trigram extension for substring/wildcard searches
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Drop default B-Tree indexes that do not support leading wildcard scans
DROP INDEX IF EXISTS idx_documents_title;
DROP INDEX IF EXISTS idx_documents_filename;

-- Create GIN trigram indexes for fast substring matches
CREATE INDEX idx_documents_title_trgm ON documents USING gin (title gin_trgm_ops);
CREATE INDEX idx_documents_filename_trgm ON documents USING gin (filename gin_trgm_ops);
