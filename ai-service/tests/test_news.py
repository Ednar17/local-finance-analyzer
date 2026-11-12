from datetime import datetime, timezone

from app.news import Source, format_digest_input, load_sources, page_url, parse_feed, strip_html
from app.retrieval import NewsHit, rank_news

SOURCE = Source("Revue test", "https://exemple.org/feed/", "diplomatie", 1, "monde", "fr", "paged")

RSS = b"""<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0"><channel><title>Test</title>
<item>
  <title>Sanctions : l&#8217;Europe durcit le ton</title>
  <link>https://exemple.org/sanctions</link>
  <pubDate>Tue, 10 Nov 2026 08:00:00 GMT</pubDate>
  <description><![CDATA[<p>Les ministres des Affaires &eacute;trang&egrave;res se r&eacute;unissent.</p>]]></description>
</item>
<item>
  <title>Article sans date</title>
  <link>https://exemple.org/sans-date</link>
</item>
</channel></rss>"""


def test_parse_feed_keeps_dated_entries_and_cleans_html():
    articles = parse_feed(RSS, SOURCE)

    assert len(articles) == 1
    article = articles[0]
    assert article.title == "Sanctions : l’Europe durcit le ton"
    assert article.summary == "Les ministres des Affaires étrangères se réunissent."
    assert article.published_at == datetime(2026, 11, 10, 8, 0, tzinfo=timezone.utc)


def test_page_url_adds_or_replaces_the_paging_parameter():
    assert page_url("https://exemple.org/feed/", "paged", 3) == "https://exemple.org/feed/?paged=3"
    assert page_url("https://exemple.org/rss?lang=fr&page=1", "page", 2) == "https://exemple.org/rss?lang=fr&page=2"


def test_strip_html():
    assert strip_html("<b>Taux</b>&nbsp;directeurs  inchangés") == "Taux directeurs inchangés"


def _hit(article_id, priority, score):
    return NewsHit(article_id, "s", "c", priority, "t", "", "u", datetime.now(timezone.utc), score)


def test_diplomatic_reviews_win_close_contests():
    ranked = rank_news([_hit(1, 2, 0.80), _hit(2, 1, 0.78), _hit(3, 2, 0.60)])
    assert [h.article_id for h in ranked] == [2, 1, 3]


def test_sources_file_lists_diplomatic_reviews_first():
    sources = load_sources()
    assert sources[0].priority == 1
    assert {s.category for s in sources if s.priority == 1} == {"diplomatie"}
    assert len({s.url for s in sources}) == len(sources)


def test_digest_input_numbers_articles():
    rows = [("IRIS", "diplomatie", 1, "Titre", "Résumé", datetime(2026, 11, 10, tzinfo=timezone.utc))]
    assert format_digest_input(rows) == "[1] (priorité 1, IRIS, 2026-11-10) Titre : Résumé"
