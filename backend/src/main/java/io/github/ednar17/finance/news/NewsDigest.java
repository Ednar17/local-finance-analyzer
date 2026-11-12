package io.github.ednar17.finance.news;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Synthèse de la revue de presse générée par l'IA locale (écrite par le service Python). */
@Entity
@Table(name = "news_digests")
public class NewsDigest {

    @Id
    private Long id;

    @Column(name = "period_start")
    private OffsetDateTime periodStart;

    @Column(name = "period_end")
    private OffsetDateTime periodEnd;

    @Column(name = "article_count")
    private Integer articleCount;

    private String content;

    private String model;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected NewsDigest() {
    }

    public Long getId() {
        return id;
    }

    public OffsetDateTime getPeriodStart() {
        return periodStart;
    }

    public OffsetDateTime getPeriodEnd() {
        return periodEnd;
    }

    public Integer getArticleCount() {
        return articleCount;
    }

    public String getContent() {
        return content;
    }

    public String getModel() {
        return model;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
