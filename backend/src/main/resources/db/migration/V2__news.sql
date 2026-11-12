-- Revue de presse mondiale : articles issus de flux RSS publics.
-- On ne stocke que le titre, le résumé fourni par le flux et le lien vers l'article original.
CREATE TABLE news_articles (
    id           BIGSERIAL PRIMARY KEY,
    source       VARCHAR(100)  NOT NULL,
    -- diplomatie (revues diplomatiques et think tanks), economie, banque_centrale
    category     VARCHAR(30)   NOT NULL,
    -- 1 = source prioritaire (revues diplomatiques), 2 = contexte économique
    priority     SMALLINT      NOT NULL,
    region       VARCHAR(30)   NOT NULL,
    language     VARCHAR(5)    NOT NULL,
    title        VARCHAR(500)  NOT NULL,
    summary      TEXT,
    url          VARCHAR(1000) NOT NULL UNIQUE,
    published_at TIMESTAMPTZ   NOT NULL,
    fetched_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    embedding    vector(768)   NOT NULL
);

CREATE INDEX idx_news_published ON news_articles (published_at DESC);
CREATE INDEX idx_news_embedding ON news_articles USING hnsw (embedding vector_cosine_ops);

-- Synthèses générées par l'IA locale à partir des articles d'une période
CREATE TABLE news_digests (
    id            BIGSERIAL PRIMARY KEY,
    period_start  TIMESTAMPTZ NOT NULL,
    period_end    TIMESTAMPTZ NOT NULL,
    article_count INTEGER     NOT NULL,
    content       TEXT        NOT NULL,
    model         VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
