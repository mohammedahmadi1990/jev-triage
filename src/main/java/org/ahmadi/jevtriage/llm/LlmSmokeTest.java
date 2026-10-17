package org.ahmadi.jevtriage.llm;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("smoke")
public class LlmSmokeTest implements CommandLineRunner {

    private final LlmTriageClient llm;

    public LlmSmokeTest(LlmTriageClient llm) { this.llm = llm; }

    @Override
    public void run(String... args) {
//        llm.debug("3 Kartons beschädigt, 2 Artikel fehlen. Bitte prüfen.");

        for (int i = 1; i <= 3; i++) {
            long start = System.nanoTime();
            var result = llm.triage("3 Kartons beschädigt, 2 Artikel fehlen. Bitte prüfen.");
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.println("LLM call " + i + ": " + ms + " ms");
            System.out.println(result);
        }

    }

}