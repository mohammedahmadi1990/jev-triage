package org.ahmadi.jevtriage.benchmark;

import org.ahmadi.jevtriage.llm.LlmTriageClient;
import org.springframework.stereotype.Component;

@Component
public class LlmStrategy implements TriageStrategy {
    private final LlmTriageClient llm;
    public LlmStrategy(LlmTriageClient llm) { this.llm = llm; }

    public String name() { return "llm"; }

    public Prediction triage(String remark) {
        var r = llm.triage(remark);
        var a = r.answer();
        // The prompt asks for "probability that the answer is correct".
        double pBlock = a.blockProbability();
        return new Prediction(r.model(),
                a.category(), a.categoryConfidence(), null,
                a.block(), pBlock,
                a.severity(), a.severityConfidence(), null,
                r.inputTokens(), r.outputTokens());
    }
}