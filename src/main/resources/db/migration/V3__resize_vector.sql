DROP TABLE IF EXISTS vector_store;

CREATE TABLE vector_store (
    id        UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding VECTOR(768)
);

CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);