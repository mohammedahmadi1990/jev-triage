package org.ahmadi.jevtriage.rules;

import org.ahmadi.jevtriage.domain.Category;
import org.ahmadi.jevtriage.domain.Severity;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Locale;

@Component
public class RuleTriageClient {

    private record Rule(Category category, List<String> keywords) {}

    // Order = priority when several problems are reported (same rule as the criteria)
    private static final List<Rule> RULES = List.of(
            new Rule(Category.DAMAGE, List.of(
                    "beschaedig", "kaputt", "defekt", "zerbrochen", "eingedrueckt", "nass", "ausgelaufen", "gerissen",
                    "damag", "broken", "crushed", "dent", "leak", "torn", "wet")),
            new Rule(Category.WRONG_ARTICLE, List.of(
                    "falsche artikel", "falscher artikel", "falsche ware", "falsch geliefert", "nicht bestellt",
                    "wrong article", "wrong item", "wrong product", "not ordered")),
            new Rule(Category.QTY_MISMATCH, List.of(
                    "fehlen", "fehlende", "zu wenig", "zu viel", "weniger als", "mehr als bestellt", "ueberliefer", "unterliefer",
                    "missing item", "items missing", "too few", "too many", "short", "quantity")),
            new Rule(Category.MISSING_DOCS, List.of(
                    "lieferschein", "papiere", "dokument", "zertifikat",
                    "delivery note", "document", "paperwork", "certificate")),
            new Rule(Category.OK, List.of(
                    "in ordnung", "alles ok", "ohne befund", "keine maengel",
                    "no issues", "all good", "looks fine"))
    );

    private static final List<String> NEGATED_DAMAGE = List.of(
            "keine schaeden", "keine beschaedigung", "unbeschaedigt", "no damage", "undamaged");

    private static final List<String> CRITICAL_HINTS = List.of(
            "totalschaden", "unbrauchbar", "komplett", "ausgelaufen", "gefahr",
            "unusable", "destroyed", "hazard", "leak");

    public RuleTriageResult triage(String remark) {
        String text = normalize(remark);

        for (Rule rule : RULES) {
            if (rule.category() == Category.DAMAGE && containsAny(text, NEGATED_DAMAGE)) continue;
            if (containsAny(text, rule.keywords())) {
                return build(rule.category(), text, true);
            }
        }
        return build(Category.OK, text, false);   // nothing matched: default, but flagged
    }

    private static RuleTriageResult build(Category category, String text, boolean matched) {
        boolean block = category == Category.DAMAGE || category == Category.WRONG_ARTICLE;
        Severity severity = switch (category) {
            case DAMAGE, WRONG_ARTICLE, QTY_MISMATCH ->
                    containsAny(text, CRITICAL_HINTS) ? Severity.CRITICAL : Severity.MEDIUM;
            case MISSING_DOCS, OK -> Severity.LOW;
        };
        return new RuleTriageResult(category, block, severity, matched);
    }

    private static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
    }

    private static boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
}