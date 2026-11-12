"""Questions-réponses sur les rapports financiers, avec la presse comme contexte humain et géopolitique."""
from . import ollama_client, prompts
from .config import settings
from .retrieval import NewsHit, ReportHit, search_news, search_reports


def format_report_context(hits: list[ReportHit]) -> str:
    return "\n\n".join(
        f"[R{i}] (document {h.document_id}, page {h.page})\n{h.content}" for i, h in enumerate(hits, start=1)
    )


def format_news_context(hits: list[NewsHit]) -> str:
    return "\n\n".join(
        f"[N{i}] {h.source} ({h.category}, {h.published_at:%Y-%m-%d}) : {h.title}\n{h.summary[:600]}"
        for i, h in enumerate(hits, start=1)
    )


def report_source(h: ReportHit) -> dict:
    return {"type": "report", "document_id": h.document_id, "page": h.page,
            "excerpt": h.content[:400], "score": round(h.score, 4)}


def news_source(h: NewsHit) -> dict:
    return {"type": "news", "article_id": h.article_id, "title": f"{h.source} : {h.title}", "url": h.url,
            "published_at": h.published_at.isoformat(), "excerpt": h.summary[:400], "score": round(h.score, 4)}


def ask_reports(question: str, document_ids: list[int], include_news: bool, top_k: int) -> dict:
    query = ollama_client.embed_query(question)
    report_hits = search_reports(query, document_ids, top_k)
    news_hits = search_news(query, top_k=4) if include_news else []
    if not report_hits and not news_hits:
        return {"answer": "Aucun document indexé ne permet de répondre. Importez d'abord un rapport.",
                "sources": [], "model": settings.chat_model}

    context = "EXTRAITS DES RAPPORTS :\n" + (format_report_context(report_hits) or "(aucun)")
    if include_news:
        context += "\n\nARTICLES DE PRESSE :\n" + (format_news_context(news_hits) or "(aucun)")
    answer = ollama_client.chat(prompts.REPORT_ANALYST, f"{context}\n\nQUESTION : {question}")
    sources = [report_source(h) for h in report_hits] + [news_source(h) for h in news_hits]
    return {"answer": answer, "sources": sources, "model": settings.chat_model}


def ask_budget(question: str, context: str, include_news: bool) -> dict:
    news_hits: list[NewsHit] = []
    if include_news:
        query = ollama_client.embed_query(question + " inflation pouvoir d'achat taux d'intérêt énergie")
        news_hits = search_news(query, top_k=4, days=90)
    user = f"DONNÉES DU BUDGET :\n{context}"
    if news_hits:
        user += "\n\nARTICLES DE PRESSE :\n" + format_news_context(news_hits)
    answer = ollama_client.chat(prompts.BUDGET_COACH, f"{user}\n\nQUESTION : {question}")
    return {"answer": answer, "sources": [news_source(h) for h in news_hits], "model": settings.chat_model}
