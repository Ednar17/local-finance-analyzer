package io.github.ednar17.finance.news;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.ednar17.finance.ai.AiDtos.NewsDigestResult;
import io.github.ednar17.finance.ai.AiDtos.NewsRefreshResult;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService service;

    public NewsController(NewsService service) {
        this.service = service;
    }

    @GetMapping
    public List<NewsArticle> search(@RequestParam(required = false) String region,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "7") int days,
                                    @RequestParam(defaultValue = "50") int limit) {
        return service.search(region, category, q, days, limit);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of("historyDays", service.historyDays(), "articlesInWindow", service.countInWindow());
    }

    @PostMapping("/refresh")
    public NewsRefreshResult refresh() {
        return service.refresh();
    }

    /** Génère une synthèse des dernières heures (24 h par défaut, 7 jours au maximum pour tenir dans le contexte du modèle). */
    @PostMapping("/digest")
    public NewsDigestResult digest(@RequestParam(defaultValue = "24") int hours) {
        return service.generateDigest(Math.min(Math.max(hours, 1), 168));
    }

    @GetMapping("/digest/latest")
    public ResponseEntity<NewsDigest> latestDigest() {
        return ResponseEntity.of(service.latestDigest());
    }

    @GetMapping("/digests")
    public List<NewsDigest> digests() {
        return service.recentDigests();
    }
}
