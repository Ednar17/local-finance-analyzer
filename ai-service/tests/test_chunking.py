import pytest

from app.chunking import chunk_pages, clean, split_text


def test_clean_joins_hyphenated_words_and_spaces():
    assert clean("chiffre d'af-\nfaires   en hausse") == "chiffre d'affaires en hausse"


def test_short_text_is_a_single_chunk():
    assert split_text("Résultat net : 1,2 Md€.", size=1000, overlap=100) == ["Résultat net : 1,2 Md€."]


def test_long_text_is_split_with_overlap_and_without_cutting_words():
    sentence = "Le chiffre d'affaires progresse de cinq pour cent sur l'exercice. "
    text = sentence * 40
    parts = split_text(text, size=300, overlap=60)

    assert len(parts) > 1
    assert all(len(p) <= 300 for p in parts)
    words = set(text.split())
    for part in parts:
        assert part.split()[0] in words and part.split()[-1] in words


def test_overlap_must_be_smaller_than_size():
    with pytest.raises(ValueError):
        split_text("abc", size=100, overlap=100)


def test_chunks_keep_their_page_number_and_skip_tiny_fragments():
    pages = ["Page de garde", "Résultats annuels : le résultat opérationnel atteint 3,4 milliards d'euros. " * 3]
    chunks = chunk_pages(pages, size=1000, overlap=100)

    assert len(chunks) == 1
    assert chunks[0].page == 2
    assert chunks[0].index == 0
