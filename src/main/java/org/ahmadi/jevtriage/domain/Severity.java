package org.ahmadi.jevtriage.domain;

public enum Severity implements DecisionOption {   // order = low to high
    LOW("low", "Low: cosmetic or administrative, no action needed today"),
    MEDIUM("medium", "Medium: needs a check or supplier contact, goods partly usable"),
    CRITICAL("critical", "Critical: goods unusable, safety risk or customer order at risk");

    private final String key;
    private final String description;

    Severity(String key, String description) { this.key = key; this.description = description; }
    public String key() { return key; }
    public String description() { return description; }
}
