package org.ahmadi.jevtriage.llm;

import org.ahmadi.jevtriage.domain.Category;
import org.ahmadi.jevtriage.domain.Severity;

public record LlmTriageAnswer(
        Category category, double categoryConfidence,
        boolean block, double blockProbability,
        Severity severity, double severityConfidence) {}