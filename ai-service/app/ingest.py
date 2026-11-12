"""Indexation d'un rapport PDF : extraction du texte page par page, découpage, embeddings, stockage pgvector."""
import io

from pypdf import PdfReader

from . import ollama_client
from .chunking import chunk_pages
from .config import settings
from .db import connection, to_vector


def extract_pages(pdf_bytes: bytes) -> list[str]:
    reader = PdfReader(io.BytesIO(pdf_bytes))
    return [(page.extract_text() or "") for page in reader.pages]


def ingest_pdf(document_id: int, pdf_bytes: bytes) -> dict:
    pages = extract_pages(pdf_bytes)
    chunks = chunk_pages(pages, settings.chunk_size, settings.chunk_overlap)
    if not chunks:
        raise ValueError("Aucun texte exploitable : le PDF est peut-être scanné (images sans OCR)")
    vectors = ollama_client.embed_documents([c.content for c in chunks])

    with connection() as conn, conn.cursor() as cur:
        # Réindexation idempotente
        cur.execute("DELETE FROM document_chunks WHERE document_id = %s", (document_id,))
        cur.executemany(
            """INSERT INTO document_chunks (document_id, page, chunk_index, content, embedding)
               VALUES (%s, %s, %s, %s, %s::vector)""",
            [(document_id, c.page, c.index, c.content, to_vector(v)) for c, v in zip(chunks, vectors)],
        )
    return {"page_count": len(pages), "chunk_count": len(chunks)}
