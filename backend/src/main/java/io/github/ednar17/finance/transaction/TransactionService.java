package io.github.ednar17.finance.transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ednar17.finance.ai.AiDtos.CategorizedTransaction;
import io.github.ednar17.finance.ai.AiDtos.TransactionToCategorize;
import io.github.ednar17.finance.ai.AiServiceClient;
import io.github.ednar17.finance.common.NotFoundException;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository repository;
    private final CsvTransactionParser parser;
    private final RuleBasedCategorizer rules;
    private final AiServiceClient aiClient;

    public TransactionService(TransactionRepository repository, CsvTransactionParser parser,
                              RuleBasedCategorizer rules, AiServiceClient aiClient) {
        this.repository = repository;
        this.parser = parser;
        this.rules = rules;
        this.aiClient = aiClient;
    }

    public record ImportReport(int read, int imported, int duplicates, int categorizedByRules,
                               int categorizedByAi, List<String> warnings) {
    }

    @Transactional
    public ImportReport importCsv(String content) {
        var parsed = parser.parse(content);
        List<String> warnings = new ArrayList<>(parsed.errors());

        Set<String> existing = parsed.transactions().isEmpty()
                ? Set.of()
                : repository.findExistingKeys(parsed.transactions().stream().map(CsvTransactionParser.ParsedTransaction::dedupKey).toList());

        List<Transaction> fresh = parsed.transactions().stream()
                .filter(p -> !existing.contains(p.dedupKey()))
                .map(p -> new Transaction(p.date(), p.label(), p.amount(), p.dedupKey()))
                .toList();

        int byRules = 0;
        List<Transaction> unknown = new ArrayList<>();
        for (Transaction t : fresh) {
            var category = rules.categorize(t.getLabel(), t.getAmount());
            if (category.isPresent()) {
                t.categorize(category.get(), Transaction.Source.RULE);
                byRules++;
            } else {
                unknown.add(t);
            }
        }
        List<Transaction> saved = repository.saveAll(fresh);

        int byAi = 0;
        if (!unknown.isEmpty()) {
            try {
                byAi = categorizeWithAi(unknown);
            } catch (RuntimeException e) {
                log.warn("Catégorisation IA indisponible : {}", e.getMessage());
                warnings.add(unknown.size() + " opérations laissées en « Autres » : le modèle local ne répond pas");
            }
        }
        return new ImportReport(parsed.transactions().size() + parsed.errors().size(), saved.size(),
                parsed.transactions().size() - fresh.size(), byRules, byAi, warnings);
    }

    private int categorizeWithAi(List<Transaction> transactions) {
        int count = 0;
        // Par lots pour rester dans la fenêtre de contexte d'un modèle 7B
        for (int i = 0; i < transactions.size(); i += 40) {
            List<Transaction> batch = transactions.subList(i, Math.min(i + 40, transactions.size()));
            Map<Long, Transaction> byId = batch.stream().collect(Collectors.toMap(Transaction::getId, Function.identity()));
            var request = batch.stream()
                    .map(t -> new TransactionToCategorize(t.getId(), t.getLabel(), t.getAmount()))
                    .toList();
            for (CategorizedTransaction r : aiClient.categorize(request, Category.codes()).results()) {
                Transaction t = byId.get(r.id());
                if (t != null) {
                    t.categorize(Category.fromCode(r.category()), Transaction.Source.AI);
                    count++;
                }
            }
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<Transaction> list(LocalDate from, LocalDate to) {
        return repository.findByOperationDateBetweenOrderByOperationDateDesc(from, to);
    }

    @Transactional
    public Transaction updateCategory(long id, Category category) {
        Transaction t = repository.findById(id).orElseThrow(() -> new NotFoundException("Opération " + id + " introuvable"));
        t.categorize(category, Transaction.Source.USER);
        return t;
    }
}
