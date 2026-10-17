package org.ahmadi.jevtriage.benchmark;

import org.ahmadi.jevtriage.domain.DatasetLoader;
import org.ahmadi.jevtriage.domain.LabeledRemark;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Profile("benchmark")
public class BenchmarkRunner implements CommandLineRunner {

    private static final String HEADER = String.join("\t",
            "strategy", "id", "split", "lang", "tags", "repeat", "status", "error", "latencyMs",
            "trueCategory", "trueBlock", "trueSeverity", "hazmat", "valueEur",
            "model", "category", "categoryProb", "categoryConfidence",
            "block", "blockProb", "severity", "severityProb", "severityConfidence",
            "inputTokens", "outputTokens");

    private final DatasetLoader loader;
    private final List<TriageStrategy> strategies;

    @Value("${benchmark.repeats:3}") private int repeats;
    @Value("${benchmark.limit:0}") private int limit;
    @Value("${benchmark.strategies:}") private List<String> only;

    public BenchmarkRunner(DatasetLoader loader, List<TriageStrategy> strategies) {
        this.loader = loader;
        this.strategies = strategies;
    }

    @Override
    public void run(String... args) throws Exception {
        var active = only.isEmpty()
                ? strategies
                : strategies.stream().filter(s -> only.contains(s.name())).toList();
        System.out.println("Strategies: " + active.stream().map(TriageStrategy::name).toList());

        List<LabeledRemark> rows = loader.load();
        if (limit > 0) rows = rows.subList(0, limit);

        // Warm-up: open connections and JIT-compile; not recorded
        for (var s : active) {
            for (int i = 0; i < 2; i++) {
                try { s.triage(rows.get(0).text()); } catch (RuntimeException ignored) {}
            }
        }

        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path out = Path.of("results", "benchmark-" + stamp + ".tsv");
        Files.createDirectories(out.getParent());

        int total = repeats * rows.size() * active.size();
        int done = 0;
        try (var w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write(HEADER);
            w.newLine();
            for (int rep = 1; rep <= repeats; rep++) {
                for (var row : rows) {
                    for (var s : active) {                 // interleaved: all active strategies per row
                        w.write(measure(s, row, rep));
                        w.newLine();
                        w.flush();
                        if (++done % 25 == 0) System.out.printf("%d / %d%n", done, total);
                    }
                }
            }
        }
        System.out.println("Results written to " + out.toAbsolutePath());
    }

    private String measure(TriageStrategy s, LabeledRemark row, int rep) {
        Prediction p = null;
        String status = "OK", error = "";
        long start = System.nanoTime();
        try {
            p = s.triage(row.text());
        } catch (RuntimeException e) {
            status = isFormatFailure(e) ? "FORMAT_FAILURE" : "ERROR";
            error = e.getClass().getSimpleName();
            System.out.println(s.name() + " " + row.id() + " " + status + ": " + e.getMessage());
        }
        double ms = (System.nanoTime() - start) / 1_000_000.0;

        List<Object> cols = new ArrayList<>(List.of(
                s.name(), row.id(), row.split(), row.lang(), String.join(",", row.tags()),
                rep, status, error, String.format(Locale.ROOT, "%.1f", ms),
                row.category(), row.block(), row.severity(), row.hazmat(), row.valueEur()));
        cols.addAll(p == null ? Collections.nCopies(11, null) : Arrays.asList(
                p.model(), p.category(), p.categoryProb(), p.categoryConfidence(),
                p.block(), p.blockProb(), p.severity(), p.severityProb(), p.severityConfidence(),
                p.inputTokens(), p.outputTokens()));

        return cols.stream().map(v -> v == null ? "" : v.toString())
                .collect(Collectors.joining("\t"));
    }

    private static boolean isFormatFailure(RuntimeException e) {
        return e instanceof IllegalStateException
                || e.getClass().getName().startsWith("tools.jackson");
    }
}