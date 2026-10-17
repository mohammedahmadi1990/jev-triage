package org.ahmadi.jevtriage.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatasetLoaderTest {

    @Test
    void datasetIsCompleteAndBalanced() throws Exception {
        var rows = new DatasetLoader().load();

        assertEquals(200, rows.size());
        assertEquals(100, rows.stream().filter(r -> r.split().equals("test")).count());

        for (Category c : Category.values()) {
            assertTrue(rows.stream().anyMatch(r -> r.category() == c), "missing " + c);
        }
    }
}