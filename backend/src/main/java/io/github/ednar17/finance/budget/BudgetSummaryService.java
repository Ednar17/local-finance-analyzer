package io.github.ednar17.finance.budget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ednar17.finance.transaction.Category;
import io.github.ednar17.finance.transaction.Transaction;
import io.github.ednar17.finance.transaction.TransactionRepository;

/** Agrégats du budget : ils servent au tableau de bord et de contexte factuel pour l'IA. */
@Service
public class BudgetSummaryService {

    private final TransactionRepository repository;

    public BudgetSummaryService(TransactionRepository repository) {
        this.repository = repository;
    }

    public record CategoryTotal(Category category, String label, BigDecimal total, BigDecimal share) {
    }

    public record MonthTotal(String month, BigDecimal income, BigDecimal expenses, BigDecimal net) {
    }

    public record LargeExpense(LocalDate date, String label, BigDecimal amount, String category) {
    }

    public record Summary(LocalDate from, LocalDate to, BigDecimal income, BigDecimal expenses, BigDecimal net,
                          BigDecimal savingsRate, List<CategoryTotal> byCategory, List<MonthTotal> byMonth,
                          List<LargeExpense> largestExpenses, int transactionCount) {
    }

    @Transactional(readOnly = true)
    public Summary summarize(LocalDate from, LocalDate to) {
        return summarize(from, to, repository.findByOperationDateBetweenOrderByOperationDateDesc(from, to));
    }

    Summary summarize(LocalDate from, LocalDate to, List<Transaction> transactions) {
        BigDecimal income = sum(transactions.stream().filter(t -> !t.isExpense()).map(Transaction::getAmount).toList());
        BigDecimal expenses = sum(transactions.stream().filter(Transaction::isExpense).map(t -> t.getAmount().negate()).toList());
        BigDecimal net = income.subtract(expenses);
        BigDecimal savingsRate = income.signum() == 0 ? BigDecimal.ZERO
                : net.multiply(BigDecimal.valueOf(100)).divide(income, 1, java.math.RoundingMode.HALF_UP);

        Map<Category, BigDecimal> perCategory = transactions.stream()
                .filter(Transaction::isExpense)
                .collect(Collectors.groupingBy(Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount().negate(), BigDecimal::add)));
        List<CategoryTotal> byCategory = perCategory.entrySet().stream()
                .sorted(Map.Entry.<Category, BigDecimal>comparingByValue().reversed())
                .map(e -> new CategoryTotal(e.getKey(), e.getKey().getLabel(), e.getValue(),
                        expenses.signum() == 0 ? BigDecimal.ZERO
                                : e.getValue().multiply(BigDecimal.valueOf(100)).divide(expenses, 1, java.math.RoundingMode.HALF_UP)))
                .toList();

        Map<YearMonth, List<Transaction>> perMonth = transactions.stream()
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getOperationDate()), TreeMap::new, Collectors.toList()));
        List<MonthTotal> byMonth = perMonth.entrySet().stream().map(e -> {
            BigDecimal in = sum(e.getValue().stream().filter(t -> !t.isExpense()).map(Transaction::getAmount).toList());
            BigDecimal out = sum(e.getValue().stream().filter(Transaction::isExpense).map(t -> t.getAmount().negate()).toList());
            return new MonthTotal(e.getKey().toString(), in, out, in.subtract(out));
        }).toList();

        List<LargeExpense> largest = transactions.stream()
                .filter(Transaction::isExpense)
                .sorted(Comparator.comparing(Transaction::getAmount))
                .limit(10)
                .map(t -> new LargeExpense(t.getOperationDate(), t.getLabel(), t.getAmount().negate(), t.getCategory().getLabel()))
                .toList();

        return new Summary(from, to, income, expenses, net, savingsRate, byCategory, byMonth, largest, transactions.size());
    }

    /** Texte compact remis au modèle : il ne voit que des agrégats, jamais le relevé brut. */
    public String toPromptContext(Summary s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Période analysée : du ").append(s.from()).append(" au ").append(s.to())
                .append(" (").append(s.transactionCount()).append(" opérations)\n");
        sb.append("Revenus : ").append(s.income()).append(" EUR ; dépenses : ").append(s.expenses())
                .append(" EUR ; solde : ").append(s.net()).append(" EUR ; taux d'épargne : ").append(s.savingsRate()).append(" %\n");
        sb.append("Dépenses par catégorie :\n");
        s.byCategory().forEach(c -> sb.append("- ").append(c.label()).append(" : ").append(c.total())
                .append(" EUR (").append(c.share()).append(" %)\n"));
        sb.append("Évolution mensuelle (revenus / dépenses / solde) :\n");
        s.byMonth().forEach(m -> sb.append("- ").append(m.month()).append(" : ").append(m.income()).append(" / ")
                .append(m.expenses()).append(" / ").append(m.net()).append("\n"));
        sb.append("Plus grosses dépenses :\n");
        s.largestExpenses().forEach(e -> sb.append("- ").append(e.date()).append(" ").append(e.label())
                .append(" : ").append(e.amount()).append(" EUR (").append(e.category()).append(")\n"));
        return sb.toString();
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
