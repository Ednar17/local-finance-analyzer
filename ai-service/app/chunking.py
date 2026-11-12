"""Découpage du texte des rapports en morceaux qui se chevauchent, en gardant le numéro de page."""
import re
from dataclasses import dataclass


@dataclass(frozen=True)
class Chunk:
    page: int
    index: int
    content: str


def clean(text: str) -> str:
    text = text.replace(" ", " ")
    text = re.sub(r"-\n(\w)", r"\1", text)          # mots coupés en fin de ligne
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def split_text(text: str, size: int, overlap: int) -> list[str]:
    """Coupe de préférence sur une fin de paragraphe ou de phrase, jamais au milieu d'un mot."""
    if overlap >= size:
        raise ValueError("overlap doit être inférieur à size")
    text = clean(text)
    if len(text) <= size:
        return [text] if text else []
    parts: list[str] = []
    start = 0
    while start < len(text):
        end = min(start + size, len(text))
        if end < len(text):
            window = text[start:end]
            cut = max(window.rfind("\n\n"), window.rfind(". "), window.rfind("\n"))
            if cut > size // 2:
                end = start + cut + 1
            else:
                space = window.rfind(" ")
                if space > size // 2:
                    end = start + space
        part = text[start:end].strip()
        if part:
            parts.append(part)
        if end >= len(text):
            break
        start = max(end - overlap, start + 1)
        # repartir au début d'un mot
        while start < len(text) and start > 0 and not text[start - 1].isspace():
            start += 1
    return parts


def chunk_pages(pages: list[str], size: int, overlap: int) -> list[Chunk]:
    chunks: list[Chunk] = []
    for page_number, page_text in enumerate(pages, start=1):
        for part in split_text(page_text, size, overlap):
            if len(part) >= 40:  # ignore les numéros de page et en-têtes isolés
                chunks.append(Chunk(page=page_number, index=len(chunks), content=part))
    return chunks
