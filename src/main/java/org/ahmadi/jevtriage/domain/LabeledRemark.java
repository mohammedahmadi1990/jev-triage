package org.ahmadi.jevtriage.domain;

import java.util.Set;

public record LabeledRemark(
        String id,
        String split,         // "calib" or "test"
        String lang,          // "de", "en", "mixed"
        String text,
        boolean hazmat,
        int valueEur,
        Category category,    // label
        boolean block,        // label
        Severity severity,    // label
        Set<String> tags      // analysis only
) {}