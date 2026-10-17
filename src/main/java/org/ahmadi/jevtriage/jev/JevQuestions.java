package org.ahmadi.jevtriage.jev;

import org.ahmadi.jevtriage.domain.DecisionOption;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

final class JevQuestions {

    static <E extends Enum<E> & DecisionOption> Map<String, Object> choice(String instructions, Class<E> type) {
        var criteria = new LinkedHashMap<String, String>();
        for (E e : type.getEnumConstants()) criteria.put(e.key(), e.description());
        return Map.of("type", "choice", "instructions", instructions, "criteria", criteria);
    }

    static <E extends Enum<E> & DecisionOption> Map<String, Object> score(String instructions, Class<E> type) {
        var levels = Arrays.stream(type.getEnumConstants()).map(DecisionOption::description).toList();
        return Map.of("type", "score", "instructions", instructions, "criteria", levels);
    }

    static Map<String, Object> noul(String statement, String whenTrue, String whenFalse) {
        return Map.of("type", "noul", "instructions", statement,
                "criteria", Map.of("true", whenTrue, "false", whenFalse));
    }
}
