# Labeling guidelines for `remarks.tsv`

All remarks are **synthetic**. No customer or company data is used. Labels were drafted with AI assistance and **reviewed by hand**.

## Columns

| Column | Meaning |
| --- | --- |
| `id` | Stable row id (`r001`–`r200`) |
| `split` | `calib` = used to choose thresholds, `test` = used only to report results (stratified 50/50 per category) |
| `lang` | `de`, `en` or `mixed` |
| `text` | The goods-receipt remark |
| `hazmat` | Known by the WMS: dangerous goods involved |
| `valueEur` | Known by the WMS: approximate value of the affected goods |
| `category` | Label: `DAMAGE`, `WRONG_ARTICLE`, `QTY_MISMATCH`, `MISSING_DOCS`, `OK` |
| `block` | Label: must the goods be blocked from putaway until checked? |
| `severity` | Label: `LOW`, `MEDIUM`, `CRITICAL` |
| `tags` | Analysis only, never sent to a model: `plain`, `indirect`, `abbrev`, `negation`, `multi`, `spelling`, `mixed`, `cosmetic`, `vague`, `distractor`, `keyword_trap` |

## Category

- **Several problems in one remark:** the most severe wins: `DAMAGE` > `WRONG_ARTICLE` > `QTY_MISMATCH` > `MISSING_DOCS`.
- **Packaging damage counts as `DAMAGE`**, even if the content is fine (then `block = false`, `severity = LOW`).
- **Spoiled or compromised goods** (temperature excursion, rust, expired batch) count as `DAMAGE` when caused in transit, and as `WRONG_ARTICLE` when the supplier shipped the wrong batch.
- **Wrong pack size with the same article** is `QTY_MISMATCH`; a different article number or specification is `WRONG_ARTICLE`.
- **Unreadable or unsigned documents** count as `MISSING_DOCS`.

## Block

- `true` for physical damage that makes putaway risky, wrong articles, and safety concerns.
- `true` for missing **safety-relevant** documents (dangerous goods papers, safety data sheets, pressure-vessel test reports).
- `true` when missing documents make it impossible to match the contents to order lines (the receipt cannot be booked).
- `false` for cosmetic packaging damage, quantity differences and other administrative issues.

## Severity

- `LOW`: cosmetic or administrative, no action needed today.
- `MEDIUM`: needs a check or supplier contact; goods partly usable.
- `CRITICAL`: goods unusable, safety risk, or a customer order or production line at risk.

## Known bias

The dataset was written **after** the rule baseline. Rows tagged `keyword_trap` deliberately contain rule keywords in a harmless context. Results for the rule baseline are therefore also reported without these rows.

## Label revisions

The first benchmark run (`results/benchmark-20261017-202326.tsv`) used the original labels (**v1**, commit `ca75392`). After that run, every row where both models disagreed with the label in the same direction was reviewed by hand (35 severity rows, 2 block rows). Most disagreements were kept because the rules above cover them and the models were simply stricter. Labels were changed only where the rule above was applied wrongly. Remark texts were not changed.

Results are reported against both **v1** and **v2** (current `remarks.tsv`). To rebuild v1, apply the `v1` column below.

| id | field | v1 | v2 | reason |
| --- | --- | --- | --- | --- |
| r011 | `block` | `false` | `true` | Contents can't be matched to order lines, so the receipt can't be booked. |
| r031 | `severity` | `MEDIUM` | `CRITICAL` | Lower steel grade (S235 instead of S355) is a safety risk if installed. |
