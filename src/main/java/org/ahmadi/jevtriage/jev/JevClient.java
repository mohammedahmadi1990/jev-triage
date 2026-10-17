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

    private static final String CATEGORY = "What kind of problem does this goods-receipt remark report? "
            + "If several, pick the most severe: damage > wrong article > quantity > documents.";
    private static final String BLOCK = "The goods must be blocked from putaway until checked.";
    private static final String SEVERITY = "How serious is the reported problem for warehouse operations?";

    private final RestClient rest;
    private final JevProperties props;

    public JevClient(RestClient jevRestClient, JevProperties props) {
        this.rest = jevRestClient;
        this.props = props;
    }

    public TriageResult triage(String remark) {
        var questions = new LinkedHashMap<String, Object>();
        questions.put("category", JevQuestions.choice(CATEGORY, Category.class));
        questions.put("block", JevQuestions.noul(BLOCK,
                "Damage, wrong articles or safety concerns make putaway risky",
                "Goods can be stored; any issue is administrative only"));
        questions.put("severity", JevQuestions.score(SEVERITY, Severity.class));

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

    private static <E extends Enum<E>> Decision<E> toScore(JevAnswer a, Class<E> type) {
        E[] levels = type.getEnumConstants();                 // "0", "1", "2" map to enum order
        var probs = new EnumMap<E, Double>(type);
        for (int i = 0; i < levels.length; i++) {
            probs.put(levels[i], a.probabilities().getOrDefault(String.valueOf(i), 0.0));
        }
        E top = probs.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        return new Decision<>(top, probs.get(top), a.confidence(), probs);
    }
}