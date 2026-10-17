package org.ahmadi.jevtriage.domain;

public interface DecisionOption {
    String key();
    String description();

    static <E extends Enum<E> & DecisionOption> E fromKey(Class<E> type, String key) {
        for (E e : type.getEnumConstants()) {
            if (e.key().equals(key)) return e;
        }
        throw new IllegalArgumentException("Unknown option: " + key);
    }
}