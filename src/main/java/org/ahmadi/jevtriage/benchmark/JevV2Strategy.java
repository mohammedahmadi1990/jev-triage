package org.ahmadi.jevtriage.benchmark;

import org.ahmadi.jevtriage.domain.TriageCriteria;
import org.ahmadi.jevtriage.jev.JevClient;
import org.springframework.stereotype.Component;

@Component
public class JevV2Strategy implements TriageStrategy {
    private final JevClient jev;
    public JevV2Strategy(JevClient jev) { this.jev = jev; }

    public String name() { return "jev-v2"; }

    public Prediction triage(String remark) {
        return JevStrategy.toPrediction(jev.triage(remark,
                TriageCriteria.BLOCK_V2, TriageCriteria.BLOCK_V2_TRUE, TriageCriteria.BLOCK_V2_FALSE));
    }
}