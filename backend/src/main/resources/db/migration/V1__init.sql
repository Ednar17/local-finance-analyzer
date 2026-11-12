CREATE EXTENSION IF NOT EXISTS vector;

-- Rapports financiers importés (PDF)
CREATE TABLE documents (
    id            BIGSERIAL PRIMARY KEY,
    company       VARCHAR(200) NOT NULL,
    fiscal_year   INTEGER      NOT NULL,
    filename      VARCHAR(300) NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    page_count    INTEGER,
    chunk_count   INTEGER,
    error_message TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Morceaux de texte vectorisés, écrits par le service IA
CREATE TABLE document_chunks (
    id          BIGSERIAL PRIMARY KEY,
    document_id BIGINT       NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    page        INTEGER      NOT NULL,
    chunk_index INTEGER      NOT NULL,
    content     TEXT         NOT NULL,
    embedding   vector(768)  NOT NULL
);

CREATE INDEX idx_chunks_document ON document_chunks (document_id);
CREATE INDEX idx_chunks_embedding ON document_chunks USING hnsw (embedding vector_cosine_ops);

-- Opérations bancaires importées (CSV)
CREATE TABLE transactions (
    id             BIGSERIAL PRIMARY KEY,
    operation_date DATE           NOT NULL,
    label          VARCHAR(500)   NOT NULL,
    amount         NUMERIC(12, 2) NOT NULL,
    category       VARCHAR(30)    NOT NULL,
    categorized_by VARCHAR(10)    NOT NULL,
    dedup_key      VARCHAR(64)    NOT NULL UNIQUE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_date ON transactions (operation_date);
