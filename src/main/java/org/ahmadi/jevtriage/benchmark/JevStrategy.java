package org.ahmadi.jevtriage.benchmark;

import org.ahmadi.jevtriage.domain.TriageResult;
import org.ahmadi.jevtriage.jev.JevClient;
import org.springframework.stereotype.Component;

@Component
public class JevStrategy implements TriageStrategy {
    private final JevClient jev;
    public JevStrategy(JevClient jev) { this.jev = jev; }

    public String name() { return "jev"; }

    public Prediction triage(String remark) {
        return toPrediction(jev.triage(remark));
    }

    /** Shared mapping, also used by JevV2Strategy. */
    static Prediction toPrediction(TriageResult r) {
        return new Prediction(r.model(),
                r.category().value(), r.category().probability(), r.category().confidence(),
                r.blockProbability() >= 0.5, r.blockProbability(),
                r.severity().value(), r.severity().probability(), r.severity().confidence(),
                r.inputTokens(), null);   // Jev output tokens are free
    }
}