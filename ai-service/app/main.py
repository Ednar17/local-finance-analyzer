"""API du service IA : indexation des rapports, RAG, catégorisation, budget et revue de presse."""
import logging
import threading

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from pydantic import BaseModel, Field

from . import categorize, ingest, news, rag
from .config import settings
from .ollama_client import OllamaError

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
log = logging.getLogger("ai-service")

app = FastAPI(title="Finance Analyzer - service IA", version="0.1.0")

_last_refresh: dict = {"status": "jamais lancée"}


class AskRequest(BaseModel):
    question: str = Field(min_length=3, max_length=2000)
    document_ids: list[int] = []
    include_news: bool = True
    top_k: int = Field(default=6, ge=1, le=20)


class TransactionIn(BaseModel):
    id: int
    label: str
    amount: float


class CategorizeRequest(BaseModel):
    transactions: list[TransactionIn]
    categories: list[str] = Field(min_length=1)


class BudgetAskRequest(BaseModel):
    question: str = Field(min_length=3, max_length=2000)
    context: str
    include_news: bool = False


class DigestRequest(BaseModel):
    hours: int = Field(default=24, ge=1, le=168)


def _ai_errors(func, *args):
    try:
        return func(*args)
    except OllamaError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "chat_model": settings.chat_model, "embed_model": settings.embed_model,
            "history_days": settings.history_days}


@app.post("/documents/ingest")
async def ingest_document(document_id: int = Form(...), file: UploadFile = File(...)) -> dict:
    content = await file.read()
    return _ai_errors(ingest.ingest_pdf, document_id, content)


@app.post("/rag/ask")
def ask(request: AskRequest) -> dict:
    return _ai_errors(rag.ask_reports, request.question, request.document_ids, request.include_news, request.top_k)


@app.post("/transactions/categorize")
def categorize_transactions(request: CategorizeRequest) -> dict:
    txs = [t.model_dump() for t in request.transactions]
    return {"results": _ai_errors(categorize.categorize, txs, request.categories)}


@app.post("/budget/ask")
def budget_ask(request: BudgetAskRequest) -> dict:
    return _ai_errors(rag.ask_budget, request.question, request.context, request.include_news)


def _run_refresh() -> None:
    _last_refresh["status"] = "en cours"
    try:
        result = news.refresh()
        _last_refresh.update(status="terminée", **result)
    except Exception as exc:
        log.exception("Collecte de presse en échec")
        _last_refresh.update(status="échec", errors=[str(exc)])


@app.post("/news/refresh")
def refresh_news(wait: bool = False) -> dict:
    """Lance la collecte. Le premier passage charge 1 an d'archives : il tourne en arrière-plan."""
    if wait:
        return news.refresh()
    if _last_refresh.get("status") != "en cours":
        _last_refresh["status"] = "en cours"
        threading.Thread(target=_run_refresh, daemon=True).start()
    return {"fetched": _last_refresh.get("fetched", 0), "inserted": _last_refresh.get("inserted", 0),
            "errors": [f"Collecte {_last_refresh['status']}"]}


@app.get("/news/status")
def news_status() -> dict:
    return _last_refresh


@app.post("/news/digest")
def digest(request: DigestRequest) -> dict:
    return _ai_errors(news.generate_digest, request.hours)
