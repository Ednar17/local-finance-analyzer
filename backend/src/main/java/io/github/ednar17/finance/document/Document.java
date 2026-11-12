package io.github.ednar17.finance.document;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Rapport financier importé (rapport annuel, résultats semestriels...). */
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String company;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected Document() {
    }

    public Document(String company, Integer fiscalYear, String filename) {
        this.company = company;
        this.fiscalYear = fiscalYear;
        this.filename = filename;
    }

    public void markIndexed(int pageCount, int chunkCount) {
        this.status = DocumentStatus.INDEXED;
        this.pageCount = pageCount;
        this.chunkCount = chunkCount;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = DocumentStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public String getCompany() {
        return company;
    }

    public Integer getFiscalYear() {
        return fiscalYear;
    }

    public String getFilename() {
        return filename;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
