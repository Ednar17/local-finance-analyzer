package io.github.ednar17.finance.transaction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.github.ednar17.finance.common.BadRequestException;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;
    private final int historyDays;

    public TransactionController(TransactionService service, @Value("${finance.history-days:365}") int historyDays) {
        this.service = service;
        this.historyDays = historyDays;
    }

    public record CategoryUpdate(@NotNull Category category) {
    }

    public record TransactionView(Long id, LocalDate date, String label, java.math.BigDecimal amount,
                                  Category category, String categoryLabel, Transaction.Source categorizedBy) {
        static TransactionView from(Transaction t) {
            return new TransactionView(t.getId(), t.getOperationDate(), t.getLabel(), t.getAmount(),
                    t.getCategory(), t.getCategory().getLabel(), t.getCategorizedBy());
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TransactionService.ImportReport importCsv(@RequestParam("file") MultipartFile file) {
        try {
            return service.importCsv(decode(file.getBytes()));
        } catch (IOException e) {
            throw new BadRequestException("Impossible de lire le fichier envoyé");
        }
    }

    /** Par défaut : les 12 derniers mois. */
    @GetMapping
    public List<TransactionView> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(historyDays) : from;
        return service.list(start, end).stream().map(TransactionView::from).toList();
    }

    @PatchMapping("/{id}/category")
    public TransactionView updateCategory(@PathVariable long id, @RequestBody CategoryUpdate update) {
        return TransactionView.from(service.updateCategory(id, update.category()));
    }

    @GetMapping("/categories")
    public List<Map<String, String>> categories() {
        return Arrays.stream(Category.values()).map(c -> Map.of("code", c.name(), "label", c.getLabel())).toList();
    }

    /** Les exports bancaires français sont souvent en Windows-1252 plutôt qu'en UTF-8. */
    static String decode(byte[] bytes) {
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        return utf8.contains("�") ? new String(bytes, java.nio.charset.Charset.forName("windows-1252")) : utf8;
    }
}
