package org.ahmadi.jevtriage.benchmark;

import org.ahmadi.jevtriage.domain.Category;
import org.ahmadi.jevtriage.domain.Severity;

/** Common result shape. Probabilities are null when an approach has none. */
public record Prediction(
        String model,
        Category category, Double categoryProb, Double categoryConfidence,
        Boolean block, Double blockProb,          // blockProb = P(block = true)
        Severity severity, Double severityProb, Double severityConfidence,
        Integer inputTokens, Integer outputTokens) {}