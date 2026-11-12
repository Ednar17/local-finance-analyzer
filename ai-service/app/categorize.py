"""Catégorisation des opérations que les règles du backend n'ont pas reconnues."""
import json
from typing import Any

from . import ollama_client, prompts


def build_prompt(transactions: list[dict], categories: list[str]) -> str:
    lines = [f'{{"id": {t["id"]}, "label": {json.dumps(t["label"], ensure_ascii=False)}, "amount": {t["amount"]}}}'
             for t in transactions]
    return "Catégories autorisées : " + ", ".join(categories) + "\n\nOpérations :\n" + "\n".join(lines)


def parse_response(raw: Any, transactions: list[dict], categories: list[str]) -> list[dict]:
    """Valide la sortie du modèle : identifiants connus, catégories autorisées, AUTRES par défaut."""
    allowed = set(categories)
    fallback = "AUTRES" if "AUTRES" in allowed else categories[-1]
    proposed: dict[int, str] = {}
    items = raw.get("results", []) if isinstance(raw, dict) else raw if isinstance(raw, list) else []
    for item in items:
        if not isinstance(item, dict):
            continue
        try:
            tx_id = int(item.get("id"))
        except (TypeError, ValueError):
            continue
        category = str(item.get("category", "")).strip().upper()
        proposed[tx_id] = category if category in allowed else fallback
    return [{"id": t["id"], "category": proposed.get(int(t["id"]), fallback)} for t in transactions]


def categorize(transactions: list[dict], categories: list[str]) -> list[dict]:
    if not transactions:
        return []
    raw = ollama_client.chat_json(prompts.CATEGORIZER, build_prompt(transactions, categories))
    return parse_response(raw, transactions, categories)
