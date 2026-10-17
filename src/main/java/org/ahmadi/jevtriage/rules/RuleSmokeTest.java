package org.ahmadi.jevtriage.rules;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@Profile("smoke")
public class RuleSmokeTest implements CommandLineRunner {

    private final RuleTriageClient rules;

    public RuleSmokeTest(RuleTriageClient rules) { this.rules = rules; }

    @Override
    public void run(String... args) {
        List<String> remarks = List.of(
                "3 Kartons beschädigt, 2 Artikel fehlen. Bitte prüfen.",
                "Lieferschein fehlt, Ware sieht gut aus.",
                "Keine Schäden erkennbar, alles in Ordnung.",
                "Palette umgekippt, Inhalt teilweise nicht mehr verwendbar.",
                "Delivered 40 pcs instead of 50.",
                "Wrong item delivered, ordered blue not red."
        );
        for (String r : remarks) {
            long start = System.nanoTime();
            var result = rules.triage(r);
            long micros = (System.nanoTime() - start) / 1_000;
            System.out.println(micros + " µs | " + r + "\n   -> " + result);
        }
    }
}