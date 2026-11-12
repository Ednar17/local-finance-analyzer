package io.github.ednar17.finance.document;

import java.time.OffsetDateTime;

public record DocumentResponse(Long id, String company, Integer fiscalYear, String filename,
                               DocumentStatus status, Integer pageCount, Integer chunkCount,
                               String errorMessage, OffsetDateTime createdAt) {

    static DocumentResponse from(Document d) {
        return new DocumentResponse(d.getId(), d.getCompany(), d.getFiscalYear(), d.getFilename(),
                d.getStatus(), d.getPageCount(), d.getChunkCount(), d.getErrorMessage(), d.getCreatedAt());
    }
}
