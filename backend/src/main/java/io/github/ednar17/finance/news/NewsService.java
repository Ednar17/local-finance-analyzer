package io.github.ednar17.finance.news;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ednar17.finance.ai.AiDtos.NewsDigestResult;
import io.github.ednar17.finance.ai.AiDtos.NewsRefreshResult;
import io.github.ednar17.finance.ai.AiServiceClient;

@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    private final NewsArticleRepository articles;
    private final NewsDigestRepository digests;
    private final AiServiceClient aiClient;
    private final int historyDays;

    public NewsService(NewsArticleRepository articles, NewsDigestRepository digests, AiServiceClient aiClient,
                       @Value("${finance.history-days:365}") int historyDays) {
        this.articles = articles;
        this.digests = digests;
        this.aiClient = aiClient;
        this.historyDays = historyDays;
    }

    @Transactional(readOnly = true)
    public List<NewsArticle> search(String region, String category, String query, int days, int limit) {
        int window = Math.min(Math.max(days, 1), historyDays);
        return articles.search(OffsetDateTime.now().minusDays(window), blankToNull(region), blankToNull(category),
                blankToNull(query), PageRequest.of(0, Math.min(Math.max(limit, 1), 200)));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional(readOnly = true)
    public long countInWindow() {
        return articles.countByPublishedAtGreaterThanEqual(OffsetDateTime.now().minusDays(historyDays));
    }

    public NewsRefreshResult refresh() {
        return aiClient.refreshNews();
    }

    public NewsDigestResult generateDigest(int hours) {
        return aiClient.generateDigest(hours);
    }

    @Transactional(readOnly = true)
    public Optional<NewsDigest> latestDigest() {
        return digests.findFirstByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<NewsDigest> recentDigests() {
        return digests.findTop10ByOrderByCreatedAtDesc();
    }

    public int historyDays() {
        return historyDays;
    }

    /** Collecte automatique toutes les deux heures (le service IA purge aussi les articles de plus d'un an). */
    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "${finance.news.refresh-interval:PT2H}")
    public void scheduledRefresh() {
        try {
            NewsRefreshResult result = refresh();
            log.info("Revue de presse : {} articles lus, {} nouveaux", result.fetched(), result.inserted());
        } catch (RuntimeException e) {
            log.warn("Collecte de la revue de presse impossible : {}", e.getMessage());
        }
    }
}
