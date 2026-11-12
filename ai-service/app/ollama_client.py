"""Client minimal de l'API Ollama (modèles exécutés en local, aucune donnée ne sort de la machine)."""
import json
from typing import Any

import httpx

from .config import settings

# nomic-embed-text distingue les textes indexés des questions grâce à ces préfixes
DOCUMENT_PREFIX = "search_document: "
QUERY_PREFIX = "search_query: "

_client = httpx.Client(base_url=settings.ollama_url, timeout=httpx.Timeout(600.0, connect=5.0))


class OllamaError(RuntimeError):
    pass


def embed(texts: list[str], batch_size: int = 32) -> list[list[float]]:
    vectors: list[list[float]] = []
    for start in range(0, len(texts), batch_size):
        batch = texts[start:start + batch_size]
        response = _post("/api/embed", {"model": settings.embed_model, "input": batch})
        vectors.extend(response["embeddings"])
    return vectors


def embed_documents(texts: list[str]) -> list[list[float]]:
    return embed([DOCUMENT_PREFIX + t for t in texts])


def embed_query(text: str) -> list[float]:
    return embed([QUERY_PREFIX + text])[0]


def chat(system: str, user: str, json_mode: bool = False, temperature: float = 0.2) -> str:
    payload: dict[str, Any] = {
        "model": settings.chat_model,
        "stream": False,
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": user},
        ],
        "options": {"temperature": temperature, "num_ctx": settings.context_window},
    }
    if json_mode:
        payload["format"] = "json"
    return _post("/api/chat", payload)["message"]["content"]


def chat_json(system: str, user: str) -> Any:
    raw = chat(system, user, json_mode=True, temperature=0.0)
    try:
        return json.loads(raw)
    except json.JSONDecodeError as exc:
        raise OllamaError(f"Réponse JSON invalide du modèle : {raw[:200]}") from exc


def _post(path: str, payload: dict[str, Any]) -> dict[str, Any]:
    try:
        response = _client.post(path, json=payload)
        response.raise_for_status()
        return response.json()
    except httpx.HTTPError as exc:
        raise OllamaError(f"Ollama indisponible ({path}) : {exc}") from exc
