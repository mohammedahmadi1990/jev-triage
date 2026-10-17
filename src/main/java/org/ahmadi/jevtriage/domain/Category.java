package org.ahmadi.jevtriage.domain;

public enum Category implements DecisionOption {
    DAMAGE("damage", "Goods or packaging are physically damaged"),
    QTY_MISMATCH("qty_mismatch", "Delivered quantity differs from the delivery note"),
    WRONG_ARTICLE("wrong_article", "An article differs from what was ordered"),
    MISSING_DOCS("missing_docs", "Delivery note or required documents are missing"),
    OK("ok", "No problem is reported");

    private final String key;
    private final String description;

    Category(String key, String description) { this.key = key; this.description = description; }
    public String key() { return key; }
    public String description() { return description; }
}