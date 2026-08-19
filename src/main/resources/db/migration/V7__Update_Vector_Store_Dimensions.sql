-- Drop the index first since it depends on the column
DROP INDEX IF EXISTS idx_vector_store_embedding;

-- Truncate existing vector store table records (existing records use 1536 dimension size)
TRUNCATE TABLE vector_store;

-- Alter the column dimension to 768 (dimension for Google text-embedding-004)
ALTER TABLE vector_store ALTER COLUMN embedding TYPE vector(768);

-- Re-create the index for similarity search optimization
CREATE INDEX idx_vector_store_embedding ON vector_store USING hnsw (embedding vector_cosine_ops);
