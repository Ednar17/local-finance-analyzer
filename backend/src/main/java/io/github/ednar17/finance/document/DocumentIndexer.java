package io.github.ednar17.finance.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.github.ednar17.finance.ai.AiServiceClient;

/** Indexe un PDF en arrière-plan : le calcul des embeddings peut prendre plusieurs minutes. */
@Component
public class DocumentIndexer {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexer.class);

    private final DocumentRepository repository;
    private final AiServiceClient aiClient;

    public DocumentIndexer(DocumentRepository repository, AiServiceClient aiClient) {
        this.repository = repository;
        this.aiClient = aiClient;
    }

    @Async
    @Transactional
    public void index(long documentId, byte[] content) {
        Document document = repository.findById(documentId).orElseThrow();
        try {
            var result = aiClient.ingestDocument(documentId, document.getFilename(), content);
            document.markIndexed(result.pageCount(), result.chunkCount());
            log.info("Document {} indexé : {} pages, {} morceaux", documentId, result.pageCount(), result.chunkCount());
        } catch (RuntimeException e) {
            log.warn("Indexation du document {} en échec", documentId, e);
            document.markFailed(e.getMessage());
        }
    }
}
