package io.github.ednar17.finance.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RuleBasedCategorizerTest {

    private final RuleBasedCategorizer categorizer = new RuleBasedCategorizer();

    @ParameterizedTest
    @CsvSource({
            "CB CARREFOUR MARKET 12/10, -45.20, ALIMENTATION",
            "PRLV SEPA EDF CLIENTS, -78.00, LOGEMENT",
            "CB UBER EATS PARIS, -23.90, RESTAURANTS",
            "CB NAVIGO ANNUEL, -86.40, TRANSPORT",
            "PRLV NETFLIX.COM, -13.49, ABONNEMENTS",
            "PRLV DGFIP IMPOT, -120.00, IMPOTS",
            "VIR SALAIRE OCTOBRE, 2150.00, REVENUS",
            "CB PHARMACIE DU CENTRE, -12.80, SANTE"
    })
    void recognisesCommonFrenchLabels(String label, BigDecimal amount, Category expected) {
        assertThat(categorizer.categorize(label, amount)).contains(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "CB SARL DUPONT ET FILS, -35.00",
            "TRANSACTION DIVERSE, -10.00",
            "VIR M MARTIN, 50.00"
    })
    void leavesUnknownLabelsForTheLocalModel(String label, BigDecimal amount) {
        assertThat(categorizer.categorize(label, amount)).isEmpty();
    }
}
