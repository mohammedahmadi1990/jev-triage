package org.ahmadi.jevtriage.domain;

import java.util.Map;

public record Decision<E extends Enum<E>>(E value, double probability, double confidence, Map<E, Double> probabilities) {}
