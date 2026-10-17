package org.ahmadi.jevtriage.jev;

import java.util.Map;

public record JevResponse(String model, Map<String, JevAnswer> answers, JevUsage usage) {}

