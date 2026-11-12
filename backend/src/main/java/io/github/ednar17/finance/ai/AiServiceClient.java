package io.github.ednar17.finance.ai;

import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import io.github.ednar17.finance.ai.AiDtos.AskRequest;
import io.github.ednar17.finance.ai.AiDtos.AskResult;
import io.github.ednar17.finance.ai.AiDtos.BudgetAskRequest;
import io.github.ednar17.finance.ai.AiDtos.BudgetAskResult;
import io.github.ednar17.finance.ai.AiDtos.CategorizeRequest;
import io.github.ednar17.finance.ai.AiDtos.CategorizeResult;
import io.github.ednar17.finance.ai.AiDtos.IngestResult;
import io.github.ednar17.finance.ai.AiDtos.NewsDigestRequest;
import io.github.ednar17.finance.ai.AiDtos.NewsDigestResult;
import io.github.ednar17.finance.ai.AiDtos.NewsRefreshResult;
import io.github.ednar17.finance.ai.AiDtos.TransactionToCategorize;
import io.github.ednar17.finance.common.AiServiceException;

/** Client HTTP du service IA : indexation des PDF, RAG, catégorisation, budget et revue de presse. */
@Component
public class AiServiceClient {

    private final RestClient restClient;

    public AiServiceClient(RestClient aiRestClient) {
        this.restClient = aiRestClient;
    }

    public IngestResult ingestDocument(long documentId, String filename, byte[] content) {
        var file = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        var body = new LinkedMultiValueMap<String, Object>();
        body.add("document_id", String.valueOf(documentId));
        body.add("file", file);
        return call("l'indexation du document " + documentId, () -> restClient.post()
                .uri("/documents/ingest")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(IngestResult.class));
    }

    public AskResult askReports(String question, List<Long> documentIds, boolean includeNews, int topK) {
        return call("la question sur les rapports", () -> restClient.post()
                .uri("/rag/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AskRequest(question, documentIds, includeNews, topK))
                .retrieve()
                .body(AskResult.class));
    }

    public CategorizeResult categorize(List<TransactionToCategorize> transactions, List<String> categories) {
        return call("la catégorisation des opérations", () -> restClient.post()
                .uri("/transactions/categorize")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CategorizeRequest(transactions, categories))
                .retrieve()
                .body(CategorizeResult.class));
    }

    public BudgetAskResult askBudget(String question, String context, boolean includeNews) {
        return call("l'analyse du budget", () -> restClient.post()
                .uri("/budget/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BudgetAskRequest(question, context, includeNews))
                .retrieve()
                .body(BudgetAskResult.class));
    }

    public NewsRefreshResult refreshNews() {
        return call("la mise à jour de la revue de presse", () -> restClient.post()
                .uri("/news/refresh")
                .retrieve()
                .body(NewsRefreshResult.class));
    }

    public NewsDigestResult generateDigest(int hours) {
        return call("la synthèse de la revue de presse", () -> restClient.post()
                .uri("/news/digest")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new NewsDigestRequest(hours))
                .retrieve()
                .body(NewsDigestResult.class));
    }

    private static <T> T call(String action, java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException e) {
            throw new AiServiceException("Le service IA a échoué pendant " + action, e);
        }
    }
}
