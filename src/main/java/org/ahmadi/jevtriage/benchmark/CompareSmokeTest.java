package org.ahmadi.jevtriage.benchmark;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@Profile("compare")
public class CompareSmokeTest implements CommandLineRunner {

    private final List<TriageStrategy> strategies;
    public CompareSmokeTest(List<TriageStrategy> strategies) { this.strategies = strategies; }

    @Override
    public void run(String... args) {
        String remark = "3 Kartons beschädigt, 2 Artikel fehlen. Bitte prüfen.";
        strategies.forEach(s -> s.triage(remark));          // warm-up
        System.out.println("\nRemark: " + remark + "\n");
        System.out.printf("%-8s %-18s %-20s %-8s %-18s %10s %12s%n",
                "strategy", "model", "category", "block", "severity", "latency", "tokens in/out");
        for (var s : strategies) {
            long t = System.nanoTime();
            Prediction p = s.triage(remark);
            double ms = (System.nanoTime() - t) / 1e6;
            System.out.printf(Locale.ROOT, "%-8s %-18s %-20s %-8s %-18s %8.1f ms %12s%n",
                    s.name(), p.model(),
                    p.category() + fmt(p.categoryProb()),
                    p.block() + fmt(p.blockProb()),
                    p.severity() + fmt(p.severityProb()),
                    ms, nz(p.inputTokens()) + "/" + nz(p.outputTokens()));
        }
    }

    private static String fmt(Double v) { return v == null ? "" : String.format(Locale.ROOT, " (%.2f)", v); }
    private static String nz(Integer v) { return v == null ? "-" : v.toString(); }
}