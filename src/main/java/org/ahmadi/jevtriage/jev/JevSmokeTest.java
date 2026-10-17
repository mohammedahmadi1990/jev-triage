package org.ahmadi.jevtriage.jev;

import org.ahmadi.jevtriage.domain.TriageResult;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("smoke")
public class JevSmokeTest implements CommandLineRunner {

    private final JevClient jev;

    public JevSmokeTest(JevClient jev) {
        this.jev = jev;
    }

    @Override
    public void run(String... args) {
        for (int i = 1; i <= 3; i++) {
            long start = System.nanoTime();
            TriageResult result = jev.triage("3 Kartons beschädigt, 2 Artikel fehlen. Bitte prüfen.");
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.println("Call " + i + ": " + ms + " ms");
            System.out.println(result);
        }
    }
}