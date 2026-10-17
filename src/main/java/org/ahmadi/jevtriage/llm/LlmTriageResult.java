package org.ahmadi.jevtriage.llm;

public record LlmTriageResult(LlmTriageAnswer answer, String model, Integer inputTokens, Integer outputTokens) {}