package io.github.ednar17.finance.ai;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Contrats JSON échangés avec le service IA (Python). */
public final class AiDtos {

    private AiDtos() {
    }

    public record IngestResult(@JsonProperty("page_count") int pageCount,
                               @JsonProperty("chunk_count") int chunkCount) {
    }

    public record AskRequest(String question,
                             @JsonProperty("document_ids") List<Long> documentIds,
                             @JsonProperty("include_news") boolean includeNews,
                             @JsonProperty("top_k") int topK) {
    }

    /**
     * Extrait cité par l'IA : un passage de rapport ("report") ou un article de presse ("news").
     * Lu en snake_case depuis Python, renvoyé en camelCase au frontend.
     */
    public record Source(String type,
                         @JsonAlias("document_id") Long documentId,
                         Integer page,
                         @JsonAlias("article_id") Long articleId,
                         String title,
                         String url,
                         @JsonAlias("published_at") OffsetDateTime publishedAt,
                         String excerpt,
                         double score) {
    }

    public record AskResult(String answer, List<Source> sources, String model) {
    }

    public record TransactionToCategorize(long id, String label, BigDecimal amount) {
    }

    public record CategorizeRequest(List<TransactionToCategorize> transactions, List<String> categories) {
    }

    public record CategorizedTransaction(long id, String category) {
    }

    public record CategorizeResult(List<CategorizedTransaction> results) {
    }

    public record BudgetAskRequest(String question, String context,
                                   @JsonProperty("include_news") boolean includeNews) {
    }

    public record BudgetAskResult(String answer, List<Source> sources, String model) {
    }

    public record NewsRefreshResult(int fetched, int inserted, List<String> errors) {
    }

    public record NewsDigestRequest(int hours) {
    }

    public record NewsDigestResult(long id, String content,
                                   @JsonAlias("article_count") int articleCount,
                                   String model) {
    }
}
