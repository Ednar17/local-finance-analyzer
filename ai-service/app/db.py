"""Accès PostgreSQL + pgvector. Le schéma est créé par les migrations Flyway du backend."""
from contextlib import contextmanager
from typing import Iterator, Sequence

from psycopg import Connection
from psycopg_pool import ConnectionPool

from .config import settings

_pool: ConnectionPool | None = None


def pool() -> ConnectionPool:
    global _pool
    if _pool is None:
        _pool = ConnectionPool(settings.database_url, min_size=1, max_size=5, open=True)
    return _pool


@contextmanager
def connection() -> Iterator[Connection]:
    with pool().connection() as conn:
        yield conn


def to_vector(values: Sequence[float]) -> str:
    """Format texte accepté par pgvector ('[0.1,0.2,...]'), converti côté SQL avec ::vector."""
    return "[" + ",".join(f"{v:.7f}" for v in values) + "]"
