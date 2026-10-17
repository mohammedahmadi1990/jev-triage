package org.ahmadi.jevtriage.jev;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JevUsage(@JsonProperty("input_tokens") int inputTokens,
                       @JsonProperty("output_tokens") int outputTokens) {}
