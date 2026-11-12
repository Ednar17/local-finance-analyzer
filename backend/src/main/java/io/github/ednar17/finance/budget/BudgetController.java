package io.github.ednar17.finance.budget;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.ednar17.finance.ai.AiDtos.BudgetAskResult;
import io.github.ednar17.finance.ai.AiServiceClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    private final BudgetSummaryService summaryService;
    private final AiServiceClient aiClient;
    private final int historyDays;

    public BudgetController(BudgetSummaryService summaryService, AiServiceClient aiClient,
                            @Value("${finance.history-days:365}") int historyDays) {
        this.summaryService = summaryService;
        this.aiClient = aiClient;
        this.historyDays = historyDays;
    }

    public record BudgetQuestion(@NotBlank @Size(max = 2000) String question,
                                 LocalDate from, LocalDate to, boolean includeNews) {
    }

    /** Par défaut : les 12 derniers mois glissants. */
    @GetMapping("/summary")
    public BudgetSummaryService.Summary summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        return summaryService.summarize(from == null ? end.minusDays(historyDays) : from, end);
    }

    @PostMapping("/ask")
    public BudgetAskResult ask(@Valid @RequestBody BudgetQuestion q) {
        LocalDate end = q.to() == null ? LocalDate.now() : q.to();
        LocalDate start = q.from() == null ? end.minusDays(historyDays) : q.from();
        var summary = summaryService.summarize(start, end);
        return aiClient.askBudget(q.question(), summaryService.toPromptContext(summary), q.includeNews());
    }
}
