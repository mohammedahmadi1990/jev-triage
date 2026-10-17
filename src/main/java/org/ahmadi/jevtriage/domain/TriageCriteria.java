package org.ahmadi.jevtriage.domain;

public final class TriageCriteria {
    public static final String CATEGORY = "What kind of problem does this goods-receipt remark report? "
            + "If several, pick the most severe: damage > wrong article > quantity > documents.";
    public static final String BLOCK = "The goods must be blocked from putaway until checked.";
    public static final String BLOCK_TRUE = "Damage, wrong articles or safety concerns make putaway risky";
    public static final String BLOCK_FALSE = "Goods can be stored; any issue is administrative only";
    public static final String SEVERITY = "How serious is the reported problem for warehouse operations?";

    private TriageCriteria() {}
}
