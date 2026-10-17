package org.ahmadi.jevtriage.domain;

public final class TriageCriteria {
    public static final String CATEGORY = "What kind of problem does this goods-receipt remark report? "
            + "If several, pick the most severe: damage > wrong article > quantity > documents.";
    public static final String BLOCK = "The goods must be blocked from putaway until checked.";
    public static final String BLOCK_TRUE = "Damage, wrong articles or safety concerns make putaway risky";
    public static final String BLOCK_FALSE = "Goods can be stored; any issue is administrative only";
    public static final String SEVERITY = "How serious is the reported problem for warehouse operations?";

    // v2: block criteria spelled out from the written labeling rule
    public static final String BLOCK_V2 =
            "Putaway must be stopped because the goods are damaged, are the wrong article, pose a safety risk, "
                    + "or lack documents needed for safety or for booking the receipt.";
    public static final String BLOCK_V2_TRUE =
            "Physical damage that makes storage risky, a wrong article, a safety concern, missing safety documents, "
                    + "or contents that cannot be matched to order lines";
    public static final String BLOCK_V2_FALSE =
            "Quantity differences, cosmetic packaging damage, missing or late routine paperwork, or no problem at all; "
                    + "the goods can be stored";

    private TriageCriteria() {}
}
