package org.ahmadi.jevtriage.domain;

public record TriageResult(Decision<Category> category, double blockProbability,
                           Decision<Severity> severity, double severityScore,
                           String model, int inputTokens) {}
