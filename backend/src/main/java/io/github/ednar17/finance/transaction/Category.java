package io.github.ednar17.finance.transaction;

import java.util.Arrays;
import java.util.List;

public enum Category {
    REVENUS("Revenus"),
    LOGEMENT("Logement"),
    ALIMENTATION("Alimentation"),
    RESTAURANTS("Restaurants"),
    TRANSPORT("Transport"),
    SANTE("Santé"),
    ABONNEMENTS("Abonnements"),
    LOISIRS("Loisirs"),
    SHOPPING("Shopping"),
    IMPOTS("Impôts et taxes"),
    EPARGNE("Épargne"),
    VIREMENTS("Virements"),
    AUTRES("Autres");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static List<String> codes() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }

    public static Category fromCode(String code) {
        try {
            return code == null ? AUTRES : Category.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AUTRES;
        }
    }
}
