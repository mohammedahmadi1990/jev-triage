package org.ahmadi.jevtriage.jev;

import org.ahmadi.jevtriage.config.JevProperties;
import org.ahmadi.jevtriage.domain.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JevClient {

    private final RestClient rest;
    private final JevProperties props;

    public JevClient(RestClient jevRestClient, JevProperties props) {
        this.rest = jevRestClient;
        this.props = props;
    }

    /** v1 block criteria, used by the original benchmark run. */
    public TriageResult triage(String remark) {
        return triage(remark, TriageCriteria.BLOCK, TriageCriteria.BLOCK_TRUE, TriageCriteria.BLOCK_FALSE);
    }

    /** Same request, with the block criteria passed in, so wording variants can be compared. */
    public TriageResult triage(String remark, String blockStatement, String whenTrue, String whenFalse) {
        var questions = new LinkedHashMap<String, Object>();
        questions.put("category", JevQuestions.choice(TriageCriteria.CATEGORY, Category.class));
        questions.put("block", JevQuestions.noul(blockStatement, whenTrue, whenFalse));
        questions.put("severity", JevQuestions.score(TriageCriteria.SEVERITY, Severity.class));

        var body = Map.of("model", props.model(), "state", remark, "questions", questions);

        JevResponse r = rest.post().uri("/v1/systemone")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JevResponse.class);

        var a = r.answers();
        return new TriageResult(
                toChoice(a.get("category"), Category.class),
                a.get("block").noul(),
                toScore(a.get("severity"), Severity.class),
                a.get("severity").score(),
                r.model(),
                r.usage().inputTokens());
    }

    private static <E extends Enum<E> & DecisionOption> Decision<E> toChoice(JevAnswer a, Class<E> type) {
        var probs = new EnumMap<E, Double>(type);
        for (E e : type.getEnumConstants()) probs.put(e, a.probabilities().getOrDefault(e.key(), 0.0));
        E top = DecisionOption.fromKey(type, a.choice());
        return new Decision<>(top, probs.get(top), a.confidence(), probs);
    }

    private static <E extends Enum<E> & DecisionOption> Decision<E> toScore(JevAnswer a, Class<E> type) {
        E[] levels = type.getEnumConstants();                 // "0", "1", "2" map to enum order
        var probs = new EnumMap<E, Double>(type);
        for (int i = 0; i < levels.length; i++) {
            probs.put(levels[i], a.probabilities().getOrDefault(String.valueOf(i), 0.0));
        }
        E top = probs.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        return new Decision<>(top, probs.get(top), a.confidence(), probs);
    }
}