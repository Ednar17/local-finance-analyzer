package io.github.ednar17.finance.document;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import io.github.ednar17.finance.common.BadRequestException;
import io.github.ednar17.finance.common.NotFoundException;

@Service
public class DocumentService {

    private final DocumentRepository repository;
    private final DocumentIndexer indexer;

    public DocumentService(DocumentRepository repository, DocumentIndexer indexer) {
        this.repository = repository;
        this.indexer = indexer;
    }

    @Transactional
    public DocumentResponse upload(MultipartFile file, String company, int fiscalYear) {
        String filename = file.getOriginalFilename() == null ? "rapport.pdf" : file.getOriginalFilename();
        if (file.isEmpty() || !filename.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Seuls les fichiers PDF non vides sont acceptés");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Impossible de lire le fichier envoyé");
        }
        Document saved = repository.save(new Document(company.trim(), fiscalYear, filename));
        long id = saved.getId();
        // L'indexation démarre une fois la ligne enregistrée en base
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                indexer.index(id, content);
            }
        });
        return DocumentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(DocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Document get(long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Document " + id + " introuvable"));
    }

    @Transactional
    public void delete(long id) {
        repository.delete(get(id));
    }
}
