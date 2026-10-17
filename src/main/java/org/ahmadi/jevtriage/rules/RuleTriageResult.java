package org.ahmadi.jevtriage.rules;

import org.ahmadi.jevtriage.domain.Category;
import org.ahmadi.jevtriage.domain.Severity;

public record RuleTriageResult(Category category, boolean block, Severity severity, boolean matched) {}