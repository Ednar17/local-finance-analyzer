"""Revue de presse : collecte des flux RSS (revues diplomatiques en priorité), historique d'un an, synthèse par l'IA."""
import html
import logging
import re
import threading
from calendar import timegm
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from urllib.parse import urlencode, urlparse, parse_qsl, urlunparse

import feedparser
import httpx
import yaml

from . import ollama_client, prompts
from .config import settings
from .db import connection, to_vector

log = logging.getLogger(__name__)

USER_AGENT = "local-finance-analyzer/0.1 (+https://github.com/Ednar17/local-finance-analyzer)"
_refresh_lock = threading.Lock()
# Sources dont les archives ont déjà été parcourues depuis le démarrage du service
_backfilled: set[str] = set()


@dataclass(frozen=True)
class Source:
    name: str
    url: str
    category: str
    priority: int
    region: str
    language: str
    paging: str | None = None


@dataclass(frozen=True)
class Article:
    source: Source
    title: str
    summary: str
    url: str
    published_at: datetime


def load_sources(path=None) -> list[Source]:
    with open(path or settings.sources_file, encoding="utf-8") as f:
        data = yaml.safe_load(f)
    sources = [Source(**s) for s in data["sources"]]
    return sorted(sources, key=lambda s: s.priority)


def strip_html(text: str) -> str:
    text = re.sub(r"<[^>]+>", " ", text or "")
    return re.sub(r"\s+", " ", html.unescape(text)).strip()


def page_url(url: str, param: str, page: int) -> str:
    parts = urlparse(url)
    query = dict(parse_qsl(parts.query))
    query[param] = str(page)
    return urlunparse(parts._replace(query=urlencode(query)))


def parse_feed(content: bytes, source: Source) -> list[Article]:
    feed = feedparser.parse(content)
    articles = []
    for entry in feed.entries:
        link = entry.get("link")
        title = strip_html(entry.get("title", ""))
        parsed = entry.get("published_parsed") or entry.get("updated_parsed")
        if not link or not title or not parsed:
            continue
        published = datetime.fromtimestamp(timegm(parsed), tz=timezone.utc)
        summary = strip_html(entry.get("summary", ""))[:1200]
        articles.append(Article(source, title[:500], summary, link[:1000], published))
    return articles


def window_start(now: datetime | None = None) -> datetime:
    return (now or datetime.now(timezone.utc)) - timedelta(days=settings.history_days)


def _known_urls(urls: list[str]) -> set[str]:
    if not urls:
        return set()
    with connection() as conn, conn.cursor() as cur:
        cur.execute("SELECT url FROM news_articles WHERE url = ANY(%s)", (urls,))
        return {row[0] for row in cur.fetchall()}


def _oldest_is_recent(name: str, start: datetime) -> bool:
    """Vrai si l'historique de la source ne couvre pas encore la fenêtre d'un an (à 30 jours près)."""
    with connection() as conn, conn.cursor() as cur:
        cur.execute("SELECT min(published_at) FROM news_articles WHERE source = %s", (name,))
        oldest = cur.fetchone()[0]
    return oldest is None or oldest > start + timedelta(days=30)


def _store(articles: list[Article]) -> int:
    fresh_urls = set(a.url for a in articles) - _known_urls([a.url for a in articles])
    fresh = [a for a in articles if a.url in fresh_urls]
    # dédoublonnage interne (un même lien peut apparaître sur deux pages d'archives)
    fresh = list({a.url: a for a in fresh}.values())
    if not fresh:
        return 0
    vectors = ollama_client.embed_documents([f"{a.title}. {a.summary}" for a in fresh])
    with connection() as conn, conn.cursor() as cur:
        cur.executemany(
            """INSERT INTO news_articles
               (source, category, priority, region, language, title, summary, url, published_at, embedding)
               VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s::vector)
               ON CONFLICT (url) DO NOTHING""",
            [(a.source.name, a.source.category, a.source.priority, a.source.region, a.source.language,
              a.title, a.summary, a.url, a.published_at, to_vector(v)) for a, v in zip(fresh, vectors)],
        )
    return len(fresh)


def _fetch(client: httpx.Client, url: str, source: Source) -> list[Article]:
    response = client.get(url)
    response.raise_for_status()
    return parse_feed(response.content, source)


def collect_source(client: httpx.Client, source: Source) -> tuple[int, int]:
    """Lit le flux courant ; au premier passage, remonte les archives jusqu'à 1 an si le flux le permet."""
    start = window_start()
    articles = [a for a in _fetch(client, source.url, source) if a.published_at >= start]
    fetched, inserted = len(articles), _store(articles)

    if source.paging and source.name not in _backfilled and _oldest_is_recent(source.name, start):
        for page in range(2, settings.backfill_max_pages + 1):
            try:
                page_articles = _fetch(client, page_url(source.url, source.paging, page), source)
            except httpx.HTTPError:
                break
            in_window = [a for a in page_articles if a.published_at >= start]
            fetched += len(in_window)
            inserted += _store(in_window)
            # fin des archives, ou on a dépassé la fenêtre d'un an
            if not page_articles or len(in_window) < len(page_articles):
                break
        _backfilled.add(source.name)
    return fetched, inserted


def purge_old() -> int:
    with connection() as conn, conn.cursor() as cur:
        cur.execute("DELETE FROM news_articles WHERE published_at < %s", (window_start(),))
        return cur.rowcount


def refresh() -> dict:
    if not _refresh_lock.acquire(blocking=False):
        return {"fetched": 0, "inserted": 0, "errors": ["Une collecte est déjà en cours"]}
    try:
        fetched = inserted = 0
        errors: list[str] = []
        with httpx.Client(timeout=20.0, follow_redirects=True, headers={"User-Agent": USER_AGENT}) as client:
            for source in load_sources():
                try:
                    f, i = collect_source(client, source)
                    fetched += f
                    inserted += i
                except Exception as exc:  # une source en panne ne bloque pas les autres
                    log.warning("Source %s en échec : %s", source.name, exc)
                    errors.append(f"{source.name} : {exc}")
        purged = purge_old()
        log.info("Revue de presse : %s lus, %s nouveaux, %s purgés (> %s jours)",
                 fetched, inserted, purged, settings.history_days)
        return {"fetched": fetched, "inserted": inserted, "errors": errors}
    finally:
        _refresh_lock.release()


def select_for_digest(hours: int, limit: int = 60) -> list[tuple]:
    since = datetime.now(timezone.utc) - timedelta(hours=hours)
    with connection() as conn, conn.cursor() as cur:
        cur.execute(
            """SELECT source, category, priority, title, coalesce(summary, ''), published_at
               FROM news_articles WHERE published_at >= %s
               ORDER BY priority ASC, published_at DESC LIMIT %s""",
            (since, limit),
        )
        return cur.fetchall()


def format_digest_input(rows: list[tuple]) -> str:
    return "\n".join(
        f"[{i}] (priorité {priority}, {source}, {published:%Y-%m-%d}) {title} : {summary[:300]}"
        for i, (source, _category, priority, title, summary, published) in enumerate(rows, start=1)
    )


def generate_digest(hours: int) -> dict:
    rows = select_for_digest(hours)
    if not rows:
        raise ValueError("Aucun article sur la période : lancez d'abord une collecte")
    content = ollama_client.chat(prompts.PRESS_REVIEW, "ARTICLES :\n" + format_digest_input(rows), temperature=0.3)
    end = datetime.now(timezone.utc)
    with connection() as conn, conn.cursor() as cur:
        cur.execute(
            """INSERT INTO news_digests (period_start, period_end, article_count, content, model)
               VALUES (%s, %s, %s, %s, %s) RETURNING id""",
            (end - timedelta(hours=hours), end, len(rows), content, settings.chat_model),
        )
        digest_id = cur.fetchone()[0]
    return {"id": digest_id, "content": content, "article_count": len(rows), "model": settings.chat_model}
