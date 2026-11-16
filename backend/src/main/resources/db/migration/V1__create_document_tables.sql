
-- V1: Initial schema for Local Knowledge AI
-- PostgreSQL 17 + pgvector
-- Embedding model: qwen3-embedding:0.6b
-- Verified embedding dimensions: 1024

-- 1. Enable pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Document metadata
CREATE TABLE document (
    id UUID PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    stored_path TEXT NOT NULL,
    file_type VARCHAR(10) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_document_file_type
        CHECK (file_type IN ('PDF', 'TXT')),

    CONSTRAINT chk_document_file_size
        CHECK (file_size > 0),

    CONSTRAINT chk_document_status
        CHECK (status IN (
            'UPLOADED',
            'PROCESSING',
            'COMPLETED',
            'FAILED'
        ))
);

-- 3. Document chunks and embeddings
CREATE TABLE document_chunk (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(1024) NOT NULL,
    page_number INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_chunk_document
        FOREIGN KEY (document_id)
        REFERENCES document(id),

    CONSTRAINT uq_document_chunk_index
        UNIQUE (document_id, chunk_index),

    CONSTRAINT chk_document_chunk_index
        CHECK (chunk_index >= 0),

    CONSTRAINT chk_document_chunk_content
        CHECK (length(btrim(content)) > 0),

    CONSTRAINT chk_document_chunk_page_number
        CHECK (page_number IS NULL OR page_number > 0)
);

-- 4. Indexes for document state and chunk lookup
CREATE INDEX idx_document_status
    ON document(status);

CREATE INDEX idx_document_chunk_document_id
    ON document_chunk(document_id);
