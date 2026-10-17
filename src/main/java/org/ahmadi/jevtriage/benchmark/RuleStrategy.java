package org.ahmadi.jevtriage.benchmark;

import org.ahmadi.jevtriage.rules.RuleTriageClient;
import org.springframework.stereotype.Component;

@Component
public class RuleStrategy implements TriageStrategy {
    private final RuleTriageClient rules;
    public RuleStrategy(RuleTriageClient rules) { this.rules = rules; }

    public String name() { return "rules"; }

    public Prediction triage(String remark) {
        var r = rules.triage(remark);
        double p = r.matched() ? 1.0 : 0.0;   // rules only know "matched" or "not matched"
        return new Prediction("rules-v1",
                r.category(), p, null,
                r.block(), r.block() ? 1.0 : 0.0,
                r.severity(), p, null,
                null, null);
    }
}