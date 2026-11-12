package io.github.ednar17.finance.report;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ednar17.finance.ai.AiDtos.AskResult;
import io.github.ednar17.finance.ai.AiDtos.Source;
import io.github.ednar17.finance.ai.AiServiceClient;
import io.github.ednar17.finance.document.DocumentRepository;
import io.github.ednar17.finance.document.Document;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Questions en langage naturel sur les rapports financiers, avec la presse comme contexte optionnel. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final AiServiceClient aiClient;
    private final DocumentRepository documents;

    public ReportController(AiServiceClient aiClient, DocumentRepository documents) {
        this.aiClient = aiClient;
        this.documents = documents;
    }

    public record AskRequest(@NotBlank @Size(max = 2000) String question,
                             List<Long> documentIds,
                             boolean includeNews) {
    }

    public record SourceView(String type, Long documentId, String company, Integer fiscalYear, Integer page,
                             Long articleId, String title, String url, String publishedAt,
                             String excerpt, double score) {
    }

    public record AskResponse(String answer, List<SourceView> sources, String model) {
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        List<Long> ids = request.documentIds() == null ? List.of() : request.documentIds();
        AskResult result = aiClient.askReports(request.question(), ids, request.includeNews(), 6);

        Map<Long, Document> byId = documents.findAllById(
                        result.sources().stream().map(Source::documentId).filter(java.util.Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(Document::getId, Function.identity()));

        List<SourceView> sources = result.sources().stream().map(s -> {
            Document d = s.documentId() == null ? null : byId.get(s.documentId());
            return new SourceView(s.type(), s.documentId(),
                    d == null ? null : d.getCompany(), d == null ? null : d.getFiscalYear(), s.page(),
                    s.articleId(), s.title(), s.url(),
                    s.publishedAt() == null ? null : s.publishedAt().toString(),
                    s.excerpt(), s.score());
        }).toList();
        return new AskResponse(result.answer(), sources, result.model());
    }
}
