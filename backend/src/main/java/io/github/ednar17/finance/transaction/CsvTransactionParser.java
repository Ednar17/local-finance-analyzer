package io.github.ednar17.finance.transaction;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import io.github.ednar17.finance.common.BadRequestException;

/**
 * Lit un export bancaire CSV. Accepte les formats courants des banques françaises :
 * séparateur ";" ou ",", dates jj/mm/aaaa ou aaaa-mm-jj, montant unique ou colonnes débit / crédit.
 */
@Component
public class CsvTransactionParser {

    public record ParsedTransaction(LocalDate date, String label, BigDecimal amount, String dedupKey) {
    }

    public record ParseResult(List<ParsedTransaction> transactions, List<String> errors) {
    }

    private static final List<String> DATE_COLUMNS = List.of("date", "date operation", "date de l'operation", "date comptable", "dateop");
    private static final List<String> LABEL_COLUMNS = List.of("libelle", "label", "description", "libelle operation", "intitule");
    private static final List<String> AMOUNT_COLUMNS = List.of("montant", "amount", "montant(eur)", "montant eur");
    private static final List<String> DEBIT_COLUMNS = List.of("debit", "debit euros", "debit (eur)");
    private static final List<String> CREDIT_COLUMNS = List.of("credit", "credit euros", "credit (eur)");

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-yyyy"));

    public ParseResult parse(String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Le fichier CSV est vide");
        }
        String text = content.startsWith("﻿") ? content.substring(1) : content;
        char delimiter = detectDelimiter(text);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (Reader reader = new StringReader(text); CSVParser parser = format.parse(reader)) {
            Map<String, Integer> header = parser.getHeaderMap();
            String dateCol = findColumn(header, DATE_COLUMNS)
                    .orElseThrow(() -> new BadRequestException("Colonne de date introuvable (attendu : date)"));
            String labelCol = findColumn(header, LABEL_COLUMNS)
                    .orElseThrow(() -> new BadRequestException("Colonne de libellé introuvable (attendu : libelle)"));
            Optional<String> amountCol = findColumn(header, AMOUNT_COLUMNS);
            Optional<String> debitCol = findColumn(header, DEBIT_COLUMNS);
            Optional<String> creditCol = findColumn(header, CREDIT_COLUMNS);
            if (amountCol.isEmpty() && debitCol.isEmpty() && creditCol.isEmpty()) {
                throw new BadRequestException("Colonne de montant introuvable (attendu : montant, ou debit et credit)");
            }

            List<ParsedTransaction> rows = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            for (CSVRecord record : parser) {
                long line = record.getRecordNumber() + 1;
                try {
                    LocalDate date = parseDate(record.get(dateCol));
                    String label = record.get(labelCol).replaceAll("\\s+", " ").trim();
                    if (label.isEmpty()) {
                        throw new IllegalArgumentException("libellé vide");
                    }
                    BigDecimal amount = amountCol.isPresent()
                            ? parseAmount(record.get(amountCol.get()))
                            : creditMinusDebit(record, debitCol, creditCol);
                    int occurrence = (int) rows.stream()
                            .filter(r -> r.date().equals(date) && r.label().equals(label) && r.amount().compareTo(amount) == 0)
                            .count();
                    rows.add(new ParsedTransaction(date, label, amount, dedupKey(date, label, amount, occurrence)));
                } catch (RuntimeException e) {
                    errors.add("Ligne " + line + " ignorée : " + e.getMessage());
                }
            }
            return new ParseResult(rows, errors);
        } catch (IOException | IllegalArgumentException e) {
            throw new BadRequestException("CSV illisible : " + e.getMessage());
        }
    }

    static char detectDelimiter(String text) {
        String firstLine = text.lines().findFirst().orElse("");
        long semicolons = firstLine.chars().filter(c -> c == ';').count();
        long commas = firstLine.chars().filter(c -> c == ',').count();
        return semicolons >= commas && semicolons > 0 ? ';' : ',';
    }

    static LocalDate parseDate(String value) {
        String v = value.trim();
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(v, f);
            } catch (DateTimeParseException ignored) {
                // format suivant
            }
        }
        throw new IllegalArgumentException("date invalide « " + value + " »");
    }

    /** Accepte "1 234,56", "-12.30", "+45,00 €"... */
    static BigDecimal parseAmount(String value) {
        String v = value.replace(" ", "").replace(" ", "").replace(" ", "").replace("€", "").replace("EUR", "").trim();
        if (v.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (v.contains(",") && v.contains(".")) {
            v = v.lastIndexOf(',') > v.lastIndexOf('.') ? v.replace(".", "").replace(',', '.') : v.replace(",", "");
        } else {
            v = v.replace(',', '.');
        }
        try {
            return new BigDecimal(v).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("montant invalide « " + value + " »");
        }
    }

    private static BigDecimal creditMinusDebit(CSVRecord record, Optional<String> debitCol, Optional<String> creditCol) {
        BigDecimal debit = debitCol.map(c -> parseAmount(record.get(c)).abs()).orElse(BigDecimal.ZERO);
        BigDecimal credit = creditCol.map(c -> parseAmount(record.get(c)).abs()).orElse(BigDecimal.ZERO);
        return credit.subtract(debit);
    }

    private static Optional<String> findColumn(Map<String, Integer> header, List<String> aliases) {
        return header.keySet().stream()
                .filter(h -> aliases.contains(normalize(h)))
                .findFirst();
    }

    static String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT).replace('_', ' ').trim();
    }

    /** Empreinte stable d'une opération : réimporter le même fichier ne crée pas de doublons. */
    static String dedupKey(LocalDate date, String label, BigDecimal amount, int occurrence) {
        String raw = date + "|" + label.toLowerCase(Locale.ROOT) + "|" + amount.toPlainString() + "|" + occurrence;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
