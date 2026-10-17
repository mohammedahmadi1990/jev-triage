package org.ahmadi.jevtriage.jev;

import java.util.Map;

public record JevAnswer(String type, String choice, Double noul, Double score,
                        Double confidence, Map<String, Double> probabilities) {}
