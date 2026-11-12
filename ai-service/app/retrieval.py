"""Recherche sémantique dans les rapports et dans la presse (similarité cosinus pgvector)."""
from dataclasses import dataclass
from datetime import datetime

from .config import settings
from .db import connection, to_vector

# Bonus de score accordé aux revues diplomatiques (priorité 1) pour qu'elles passent devant
PRIORITY_BOOST = {1: 0.05, 2: 0.0}


@dataclass
class ReportHit:
    document_id: int
    page: int
    content: str
    score: float


@dataclass
class NewsHit:
    article_id: int
    source: str
    category: str
    priority: int
    title: str
    summary: str
    url: str
    published_at: datetime
    score: float


def search_reports(query_vector: list[float], document_ids: list[int], top_k: int) -> list[ReportHit]:
    sql = """SELECT document_id, page, content, 1 - (embedding <=> %s::vector) AS score
             FROM document_chunks"""
    params: list = [to_vector(query_vector)]
    if document_ids:
        sql += " WHERE document_id = ANY(%s)"
        params.append(document_ids)
    sql += " ORDER BY embedding <=> %s::vector LIMIT %s"
    params += [to_vector(query_vector), top_k]
    with connection() as conn, conn.cursor() as cur:
        cur.execute(sql, params)
        return [ReportHit(*row) for row in cur.fetchall()]


def search_news(query_vector: list[float], top_k: int, days: int | None = None) -> list[NewsHit]:
    window = min(days or settings.history_days, settings.history_days)
    vec = to_vector(query_vector)
    # On présélectionne large puis on applique le bonus de priorité
    sql = """SELECT id, source, category, priority, title, coalesce(summary, ''), url, published_at,
                    1 - (embedding <=> %s::vector) AS score
             FROM news_articles
             WHERE published_at >= now() - make_interval(days => %s)
             ORDER BY embedding <=> %s::vector
             LIMIT %s"""
    with connection() as conn, conn.cursor() as cur:
        cur.execute(sql, (vec, window, vec, top_k * 4))
        hits = [NewsHit(*row) for row in cur.fetchall()]
    return rank_news(hits)[:top_k]


def rank_news(hits: list[NewsHit]) -> list[NewsHit]:
    return sorted(hits, key=lambda h: h.score + PRIORITY_BOOST.get(h.priority, 0.0), reverse=True)
