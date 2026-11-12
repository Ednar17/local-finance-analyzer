package io.github.ednar17.finance.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.ednar17.finance.transaction.Category;
import io.github.ednar17.finance.transaction.Transaction;
import io.github.ednar17.finance.transaction.TransactionRepository;

class BudgetSummaryServiceTest {

    private final BudgetSummaryService service = new BudgetSummaryService(mock(TransactionRepository.class));

    private static Transaction tx(String date, String label, String amount, Category category) {
        Transaction t = new Transaction(LocalDate.parse(date), label, new BigDecimal(amount), label + date);
        t.categorize(category, Transaction.Source.RULE);
        return t;
    }

    private final List<Transaction> sample = List.of(
            tx("2026-09-01", "SALAIRE", "2000.00", Category.REVENUS),
            tx("2026-09-03", "LOYER", "-800.00", Category.LOGEMENT),
            tx("2026-09-10", "CARREFOUR", "-200.00", Category.ALIMENTATION),
            tx("2026-10-01", "SALAIRE", "2000.00", Category.REVENUS),
            tx("2026-10-03", "LOYER", "-800.00", Category.LOGEMENT),
            tx("2026-10-12", "RESTAURANT", "-200.00", Category.RESTAURANTS));

    @Test
    void computesTotalsAndSavingsRate() {
        var summary = service.summarize(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-31"), sample);

        assertThat(summary.income()).isEqualByComparingTo("4000.00");
        assertThat(summary.expenses()).isEqualByComparingTo("2000.00");
        assertThat(summary.net()).isEqualByComparingTo("2000.00");
        assertThat(summary.savingsRate()).isEqualByComparingTo("50.0");
    }

    @Test
    void ranksCategoriesByAmountWithTheirShare() {
        var summary = service.summarize(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-31"), sample);

        assertThat(summary.byCategory().get(0).category()).isEqualTo(Category.LOGEMENT);
        assertThat(summary.byCategory().get(0).share()).isEqualByComparingTo("80.0");
    }

    @Test
    void groupsByMonthInChronologicalOrder() {
        var summary = service.summarize(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-31"), sample);

        assertThat(summary.byMonth()).extracting(BudgetSummaryService.MonthTotal::month)
                .containsExactly("2026-09", "2026-10");
        assertThat(summary.byMonth().get(1).net()).isEqualByComparingTo("1000.00");
    }

    @Test
    void promptContextContainsOnlyAggregates() {
        var summary = service.summarize(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-10-31"), sample);

        String context = service.toPromptContext(summary);

        assertThat(context).contains("taux d'épargne : 50.0 %").contains("Logement : 1600.00 EUR");
    }

    @Test
    void handlesAnEmptyPeriod() {
        var summary = service.summarize(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), List.of());

        assertThat(summary.savingsRate()).isEqualByComparingTo("0");
        assertThat(summary.byCategory()).isEmpty();
    }
}
