package io.github.ednar17.finance.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import io.github.ednar17.finance.common.BadRequestException;

class CsvTransactionParserTest {

    private final CsvTransactionParser parser = new CsvTransactionParser();

    @Test
    void parsesFrenchBankExportWithSemicolonsAndCommaDecimals() {
        String csv = """
                Date;Libellé;Montant
                03/10/2026;CARTE CARREFOUR MARKET;-54,30
                05/10/2026;VIR SALAIRE OCTOBRE;2 150,00
                """;

        var result = parser.parse(csv);

        assertThat(result.errors()).isEmpty();
        assertThat(result.transactions()).hasSize(2);
        var first = result.transactions().get(0);
        assertThat(first.date()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(first.label()).isEqualTo("CARTE CARREFOUR MARKET");
        assertThat(first.amount()).isEqualByComparingTo("-54.30");
        assertThat(result.transactions().get(1).amount()).isEqualByComparingTo("2150.00");
    }

    @Test
    void supportsSeparateDebitAndCreditColumns() {
        String csv = """
                date,libelle,debit,credit
                2026-09-01,LOYER SEPTEMBRE,850.00,
                2026-09-02,REMBOURSEMENT AMELI,,23.50
                """;

        var result = parser.parse(csv);

        assertThat(result.transactions()).extracting(CsvTransactionParser.ParsedTransaction::amount)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("-850.00"), new BigDecimal("23.50"));
    }

    @Test
    void reportsInvalidLinesWithoutFailingTheWholeImport() {
        String csv = """
                Date;Libellé;Montant
                pas une date;CARTE FNAC;-20,00
                10/10/2026;CARTE FNAC;-20,00
                """;

        var result = parser.parse(csv);

        assertThat(result.transactions()).hasSize(1);
        assertThat(result.errors()).singleElement().asString().contains("Ligne 2");
    }

    @Test
    void identicalOperationsOnTheSameDayGetDistinctKeys() {
        String csv = """
                Date;Libellé;Montant
                10/10/2026;CARTE RATP;-2,15
                10/10/2026;CARTE RATP;-2,15
                """;

        var result = parser.parse(csv);

        assertThat(result.transactions().get(0).dedupKey()).isNotEqualTo(result.transactions().get(1).dedupKey());
    }

    @Test
    void rejectsFileWithoutAmountColumn() {
        assertThatThrownBy(() -> parser.parse("Date;Libellé\n10/10/2026;TEST\n"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("montant");
    }

    @Test
    void parsesAmountsWithThousandsSeparators() {
        assertThat(CsvTransactionParser.parseAmount("1.234,56 €")).isEqualByComparingTo("1234.56");
        assertThat(CsvTransactionParser.parseAmount("1,234.56")).isEqualByComparingTo("1234.56");
        assertThat(CsvTransactionParser.parseAmount("-12,3")).isEqualByComparingTo("-12.30");
    }
}
