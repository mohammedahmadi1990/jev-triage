package org.ahmadi.jevtriage.benchmark;

public interface TriageStrategy {
    String name();
    Prediction triage(String remark);
}