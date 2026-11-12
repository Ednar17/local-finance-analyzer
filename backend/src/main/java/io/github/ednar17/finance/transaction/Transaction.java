package io.github.ednar17.finance.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Opération bancaire. Montant négatif pour une dépense, positif pour une rentrée d'argent. */
@Entity
@Table(name = "transactions")
public class Transaction {

    public enum Source { RULE, AI, USER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_date", nullable = false)
    private LocalDate operationDate;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category = Category.AUTRES;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorized_by", nullable = false)
    private Source categorizedBy = Source.RULE;

    @Column(name = "dedup_key", nullable = false, unique = true)
    private String dedupKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected Transaction() {
    }

    public Transaction(LocalDate operationDate, String label, BigDecimal amount, String dedupKey) {
        this.operationDate = operationDate;
        this.label = label;
        this.amount = amount;
        this.dedupKey = dedupKey;
    }

    public void categorize(Category category, Source source) {
        this.category = category;
        this.categorizedBy = source;
    }

    public boolean isExpense() {
        return amount.signum() < 0;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getOperationDate() {
        return operationDate;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Category getCategory() {
        return category;
    }

    public Source getCategorizedBy() {
        return categorizedBy;
    }

    public String getDedupKey() {
        return dedupKey;
    }
}
