"""Configuration lue dans les variables d'environnement."""
import os
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Settings:
    database_url: str = os.getenv("DATABASE_URL", "postgresql://finance:finance@localhost:5432/finance")
    ollama_url: str = os.getenv("OLLAMA_URL", "http://localhost:11434")
    chat_model: str = os.getenv("CHAT_MODEL", "qwen2.5:7b")
    embed_model: str = os.getenv("EMBED_MODEL", "nomic-embed-text")
    # Fenêtre glissante d'historique : la presse est chargée et conservée sur 1 an
    history_days: int = int(os.getenv("HISTORY_DAYS", "365"))
    # Nombre maximal de pages d'archives lues par flux lors du premier chargement
    backfill_max_pages: int = int(os.getenv("BACKFILL_MAX_PAGES", "60"))
    sources_file: Path = Path(os.getenv("SOURCES_FILE", Path(__file__).resolve().parent.parent / "sources.yaml"))
    chunk_size: int = 1000
    chunk_overlap: int = 150
    context_window: int = int(os.getenv("CONTEXT_WINDOW", "8192"))


settings = Settings()
