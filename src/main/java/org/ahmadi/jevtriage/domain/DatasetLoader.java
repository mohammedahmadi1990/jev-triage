package org.ahmadi.jevtriage.domain;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Component
public class DatasetLoader {

    public List<LabeledRemark> load() throws IOException {
        var resource = new ClassPathResource("data/remarks.tsv");
        try (var reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .map(line -> line.split("\t", -1))
                    .map(c -> new LabeledRemark(
                            c[0], c[1], c[2], c[3],
                            Boolean.parseBoolean(c[4]),
                            Integer.parseInt(c[5]),
                            Category.valueOf(c[6]),
                            Boolean.parseBoolean(c[7]),
                            Severity.valueOf(c[8]),
                            Set.of(c[9].split(","))))
                    .toList();
        }
    }
}