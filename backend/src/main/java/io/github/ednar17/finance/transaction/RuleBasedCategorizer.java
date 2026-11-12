package io.github.ednar17.finance.transaction;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Première passe de catégorisation par mots-clés : rapide, déterministe et explicable.
 * Seules les opérations non reconnues sont envoyées au modèle local.
 */
@Component
public class RuleBasedCategorizer {

    private static final Map<Category, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put(Category.IMPOTS, List.of("dgfip", "impot", "taxe fonciere", "taxe habitation", "urssaf", "amende"));
        KEYWORDS.put(Category.EPARGNE, List.of("livret a", "ldds", "assurance vie", "pea ", "epargne"));
        KEYWORDS.put(Category.LOGEMENT, List.of("loyer", "edf", "engie", "total energies", "veolia", "suez", "assurance habitation", "syndic", "foncia", "nexity"));
        KEYWORDS.put(Category.ALIMENTATION, List.of("carrefour", "leclerc", "auchan", "intermarche", "lidl", "aldi", "monoprix", "franprix", "casino", "picard", "super u", "grand frais", "biocoop"));
        KEYWORDS.put(Category.RESTAURANTS, List.of("restaurant", "mcdonald", "burger king", "kfc", "uber eats", "deliveroo", "starbucks", "boulangerie", "brasserie", "sushi", "pizza"));
        KEYWORDS.put(Category.TRANSPORT, List.of("sncf", "ratp", "navigo", "uber", "bolt", "total", "esso", "shell", "bp ", "peage", "vinci autoroutes", "blablacar", "lime", "air france", "easyjet"));
        KEYWORDS.put(Category.SANTE, List.of("pharmacie", "docteur", "dr ", "medecin", "dentiste", "mutuelle", "ameli", "cpam", "hopital", "laboratoire"));
        KEYWORDS.put(Category.ABONNEMENTS, List.of("netflix", "spotify", "deezer", "disney", "canal+", "amazon prime", "free mobile", "orange", "sfr", "bouygues", "sosh", "red by sfr", "apple.com", "google storage", "icloud", "basic fit", "fitness park"));
        KEYWORDS.put(Category.LOISIRS, List.of("cinema", "ugc", "pathe", "fnac", "steam", "playstation", "theatre", "concert", "musee", "decathlon"));
        KEYWORDS.put(Category.SHOPPING, List.of("amazon", "zalando", "zara", "h&m", "uniqlo", "ikea", "leroy merlin", "castorama", "darty", "boulanger", "cdiscount", "vinted", " action "));
        KEYWORDS.put(Category.VIREMENTS, List.of("vir ", "virement", "lydia", "paypal", "wero"));
    }

    private static final List<String> INCOME_KEYWORDS = List.of("salaire", "paie", "remuneration", "caf", "pole emploi", "france travail", "remboursement", "prime", "virement recu", "vir recu");

    public Optional<Category> categorize(String label, BigDecimal amount) {
        String text = " " + CsvTransactionParser.normalize(label) + " ";
        if (amount.signum() > 0) {
            boolean income = INCOME_KEYWORDS.stream().anyMatch(text::contains);
            return income ? Optional.of(Category.REVENUS) : Optional.empty();
        }
        return KEYWORDS.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(text::contains))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
