package io.github.ednar17.finance.news;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Article de presse collecté par le service IA. Lecture seule côté Java :
 * l'écriture et la vectorisation sont faites en Python (la colonne embedding n'est pas mappée).
 */
@Entity
@Table(name = "news_articles")
public class NewsArticle {

    @Id
    private Long id;

    private String source;

    private String category;

    private Short priority;

    private String region;

    private String language;

    private String title;

    private String summary;

    private String url;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    protected NewsArticle() {
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getCategory() {
        return category;
    }

    public Short getPriority() {
        return priority;
    }

    public String getRegion() {
        return region;
    }

    public String getLanguage() {
        return language;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getUrl() {
        return url;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
}
