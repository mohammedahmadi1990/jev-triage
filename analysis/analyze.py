"""Analysis of jev-triage benchmark results.
Usage: python analyze.py results/benchmark-XXXX.tsv
"""
import sys
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import os

import argparse
ap = argparse.ArgumentParser()
ap.add_argument("results", nargs="+", help="one or more benchmark results TSVs (combined)")
ap.add_argument("--out", default=".", help="output folder for charts")
ap.add_argument("--labels", help="remarks.tsv whose labels override those recorded at run time")
args = ap.parse_args()
PATH, OUT = args.results, args.out
os.makedirs(OUT, exist_ok=True)
PRICES = {  # USD per 1M tokens (input, output), as of Oct 2026
    "jev": (0.042, 0.0),
    "llm": (0.10, 0.50),   # Claude Haiku 5.5, prompts <= 100k tokens
    "rules": (0.0, 0.0),
    "jev-v2": (0.042, 0.0),
}
ERROR_BUDGET = 0.02     # max share of wrong automatic decisions
HIGH_VALUE_EUR = 5000

df = pd.concat([pd.read_csv(p, sep="\t") for p in PATH], ignore_index=True)
if args.labels:   # use the current labels instead of the ones recorded at run time
    lab = pd.read_csv(args.labels, sep="\t", dtype=str).set_index("id")
    df["trueCategory"] = df.id.map(lab.category)
    df["trueBlock"] = df.id.map(lab.block)
    df["trueSeverity"] = df.id.map(lab.severity)
    print(f"Labels taken from {args.labels}")
df["tags"] = df["tags"].fillna("")
df = df[df.status == "OK"].copy()
for c in ["trueBlock", "block", "hazmat"]:
    df[c] = df[c].astype(str).str.lower() == "true"

df["catOk"] = df.category == df.trueCategory
df["blockOk"] = df.block == df.trueBlock
df["sevOk"] = df.severity == df.trueSeverity
df["decisionOk"] = df.catOk & df.blockOk          # what an automatic action depends on
df["cost"] = df.apply(lambda r: (np.nan_to_num(r.inputTokens) * PRICES[r.strategy][0]
                                 + np.nan_to_num(r.outputTokens) * PRICES[r.strategy][1]) / 1e6, axis=1)

S = [x for x in ["rules", "jev", "jev-v2", "llm"] if x in set(df.strategy)]
JEVS = [x for x in ["jev", "jev-v2"] if x in S]
test = df[df.split == "test"]
calib = df[df.split == "calib"]

def pct(x): return f"{100 * x:.1f}"

print("=" * 70, "\n1. ACCURACY (test split, mean over 3 repeats; range across repeats)\n")
rows = []
for s in S:
    t = test[test.strategy == s]
    per_rep = t.groupby("repeat")[["catOk", "blockOk", "sevOk", "decisionOk"]].mean()
    rows.append([s] + [f"{pct(per_rep[c].mean())} ({pct(per_rep[c].min())}-{pct(per_rep[c].max())})"
                       for c in ["catOk", "blockOk", "sevOk", "decisionOk"]])
print(pd.DataFrame(rows, columns=["strategy", "category %", "block %", "severity %", "cat+block %"]).to_string(index=False))

print("\n", "=" * 70, "\n2. LATENCY and COST (all rows)\n")
rows = []
for s in S:
    t = df[df.strategy == s]
    rows.append([s, f"{t.latencyMs.median():.1f}", f"{t.latencyMs.quantile(.95):.1f}",
                 f"{t.latencyMs.max():.1f}",
                 f"{t.inputTokens.mean():.0f}" if t.inputTokens.notna().any() else "-",
                 f"{t.outputTokens.mean():.0f}" if t.outputTokens.notna().any() else "-",
                 f"{t.cost.mean() * 1000:.4f}", f"{t.cost.mean() * 100000:.2f}"])
print(pd.DataFrame(rows, columns=["strategy", "p50 ms", "p95 ms", "max ms", "avg in tok", "avg out tok",
                                  "$ per 1k", "$ per 100k"]).to_string(index=False))

print("\n", "=" * 70, "\n3. CALIBRATION - category (test split)\n")
bins = [0, 0.6, 0.9, 0.99, 1.0001]
labels = ["<0.60", "0.60-0.90", "0.90-0.99", ">=0.99"]
def calib_table(t, col, name):
    x = t[[col, "catOk"]].dropna()
    x["bucket"] = pd.cut(x[col], bins=bins, labels=labels, right=False)
    g = x.groupby("bucket", observed=False).agg(n=("catOk", "size"), acc=("catOk", "mean"), avgp=(col, "mean"))
    g["share %"] = (100 * g.n / len(x)).round(1)
    g["accuracy %"] = (100 * g.acc).round(1)
    g["avg stated %"] = (100 * g.avgp).round(1)
    ece = (abs(x.groupby("bucket", observed=False)[col].mean() - x.groupby("bucket", observed=False).catOk.mean())
           * x.groupby("bucket", observed=False).size() / len(x)).sum()
    print(f"-- {name}  (ECE = {ece:.3f})")
    print(g[["n", "share %", "avg stated %", "accuracy %"]].to_string(), "\n")
calib_table(test[test.strategy == "jev"], "categoryProb", "Jev top-option probability")
calib_table(test[test.strategy == "jev"], "categoryConfidence", "Jev entropy confidence")
if "jev-v2" in S: calib_table(test[test.strategy == "jev-v2"], "categoryProb", "Jev v2 top-option probability")
calib_table(test[test.strategy == "llm"], "categoryProb", "LLM verbalized confidence")

print("=" * 70, "\n4. BLOCK probability quality (test split)\n")
for s in JEVS + ["llm"]:
    t = test[test.strategy == s]
    brier = ((t.blockProb - t.trueBlock.astype(float)) ** 2).mean()
    incons = ((t.block != (t.blockProb >= 0.5))).mean()
    print(f"{s}: Brier score = {brier:.3f} (lower is better), block/prob contradictions = {pct(incons)} %")

print("\n", "=" * 70, "\n5. REPEAT VARIANCE (all rows)\n")
for s in S:
    t = df[df.strategy == s]
    flips = t.groupby("id").category.nunique().gt(1).mean()
    sflips = t.groupby("id").severity.nunique().gt(1).mean()
    bflips = t.groupby("id").block.nunique().gt(1).mean()
    pstd = t.groupby("id").categoryProb.std().mean()
    print(f"{s:6s} remarks with changing category: {pct(flips)} %, block: {pct(bflips)} %, "
          f"severity: {pct(sflips)} %, avg std of category prob: {pstd:.3f}")

print("\n", "=" * 70, "\n6. CATEGORY ACCURACY by language and tag (test split)\n")
print((100 * test.groupby(["lang", "strategy"]).catOk.mean().unstack()).round(1).to_string(), "\n")
tag_rows = []
for tag in sorted({t for ts in df.tags for t in ts.split(",") if t}):
    tt = test[test.tags.str.split(",").apply(lambda l: tag in l)]
    if tt.empty: continue
    tag_rows.append([tag, tt.id.nunique()] + [round(100 * tt[tt.strategy == s].catOk.mean(), 1) for s in S])
print(pd.DataFrame(tag_rows, columns=["tag", "remarks"] + S).to_string(index=False))

print("\n", "=" * 70, "\n7. RULES: matched vs. unmatched (test split)\n")
r = test[test.strategy == "rules"]
m = r[r.categoryProb == 1.0]
print(f"matched: {pct(len(m) / len(r))} % of remarks, accuracy when matched: {pct(m.catOk.mean())} %")
nt = r[~r.tags.str.contains("keyword_trap")]
print(f"category accuracy overall: {pct(r.catOk.mean())} %, without keyword_trap rows: {pct(nt.catOk.mean())} %")

# ---------------------------------------------------------------- block threshold
print("\n", "=" * 70, "\n8. BLOCK THRESHOLD tuned on calib, applied to test\n")
from sklearn.metrics import roc_auc_score
THR = {}
for s in JEVS + ["llm"]:
    c, t = calib[calib.strategy == s], test[test.strategy == s]
    grid = np.round(np.arange(0.30, 0.96, 0.01), 2)
    THR[s] = max(grid, key=lambda th: ((c.blockProb >= th) == c.trueBlock).mean())
    print(f"{s}: AUC {roc_auc_score(t.trueBlock, t.blockProb):.3f}, "
          f"test accuracy at 0.50 = {pct(((t.blockProb >= .5) == t.trueBlock).mean())} %, "
          f"tuned threshold {THR[s]:.2f} -> {pct(((t.blockProb >= THR[s]) == t.trueBlock).mean())} %")
print("\nMean P(block) by true category (all rows):")
print(df[df.strategy != "rules"].pivot_table(index="trueCategory", columns="strategy",
                                             values="blockProb", aggfunc="mean").round(2).to_string())

print("\n", "=" * 70, "\n9. SEVERITY: confusion matrices (all rows) and model agreement\n")
for s in JEVS + ["llm"]:
    t = df[df.strategy == s]
    print(s); print(pd.crosstab(t.trueSeverity, t.severity).to_string(), "\n")
jj = df[df.strategy == "jev"].set_index(["id", "repeat"]); ll = df[df.strategy == "llm"].set_index(["id", "repeat"])
print(f"Jev and LLM give the same severity: {pct((jj.severity == ll.severity).mean())} %; "
      f"both agree with each other but not with the label: "
      f"{pct(((jj.severity == ll.severity) & (jj.severity != jj.trueSeverity)).mean())} %")

# ---------------------------------------------------------------- automation / hybrid
print("\n", "=" * 70, f"\n10. SAFE AUTOMATION (error budget {pct(ERROR_BUDGET)} %, bands tuned on calib, reported on test)\n")
print("A decision is automated only if P(block) is outside an uncertainty band [lo, hi];")
print("it counts as an error if the category or the block decision is wrong.\n")

def wide(part):
    j = part[part.strategy == "jev"].set_index(["id", "repeat"])
    l = part[part.strategy == "llm"].set_index(["id", "repeat"])
    return j.join(l[["category", "blockProb", "latencyMs", "cost"]], rsuffix="_llm")

wc, wt = wide(calib), wide(test)
BANDS = [(lo, hi) for lo in np.round(np.arange(0.05, 0.80, 0.05), 2)
         for hi in np.round(np.arange(0.50, 1.0, 0.02), 2) if hi > lo]

def decide(p, lo, hi, mid):
    sure = (p <= lo) | (p >= hi)
    return sure, p >= mid

def policy(w, name, lo, hi):
    """Returns automated mask, correct mask, llm-called mask."""
    cat_ok_j = w.category == w.trueCategory
    if name == "Jev alone":
        sure, blk = decide(w.blockProb, lo, hi, THR["jev"])
        return sure, cat_ok_j & (blk == w.trueBlock), pd.Series(False, index=w.index)
    if name == "LLM alone":
        sure, blk = decide(w.blockProb_llm, lo, hi, THR["llm"])
        return sure, (w.category_llm == w.trueCategory) & (blk == w.trueBlock), pd.Series(True, index=w.index)
    sure, blk_j = decide(w.blockProb, lo, hi, THR["jev"])
    if name == "Jev, LLM decides when unsure":
        blk_l = w.blockProb_llm >= THR["llm"]
        ok = (cat_ok_j & (blk_j == w.trueBlock) & sure) | \
             ((w.category_llm == w.trueCategory) & (blk_l == w.trueBlock) & ~sure)
        return pd.Series(True, index=w.index), ok, ~sure
    # hybrid: Jev first; if unsure, LLM second opinion must agree with Jev's best guess
    blk_l = w.blockProb_llm >= THR["llm"]
    agree = (w.category == w.category_llm) & (blk_j == blk_l)
    auto = sure | (~sure & agree)
    called = ~sure
    if name == "Hybrid + risk gate":
        risky = w.hazmat | (w.valueEur >= HIGH_VALUE_EUR) | (w.severity == "CRITICAL")
        auto, called = auto & ~risky, called & ~risky
    return auto, cat_ok_j & (blk_j == w.trueBlock), called

def stats(w, name, lo, hi):
    mid = THR["llm"] if name == "LLM alone" else THR["jev"]
    if not (lo <= mid <= hi):          # the band must contain the decision threshold
        return 0.0, 1.0, pd.Series(False, index=w.index)
    auto, ok, called = policy(w, name, lo, hi)
    err = ((~ok) & auto).sum() / auto.sum() if auto.sum() else 0.0
    return auto.mean(), err, called

POLICIES = ["Jev alone", "LLM alone", "Jev + LLM second opinion", "Jev, LLM decides when unsure", "Hybrid + risk gate"]
rows, curves = [], {}
for name in POLICIES:
    feasible = [(stats(wc, name, lo, hi)[0], lo, hi) for lo, hi in BANDS
                if stats(wc, name, lo, hi)[1] <= ERROR_BUDGET]
    cov_c, lo, hi = max(feasible) if feasible else (0, 0, 1)
    cov, err, called = stats(wt, name, lo, hi)
    if name == "LLM alone":
        lat, cost = wt.latencyMs_llm, wt.cost_llm
    else:
        lat = wt.latencyMs + np.where(called, wt.latencyMs_llm, 0)
        cost = wt.cost + np.where(called, wt.cost_llm, 0)
    rows.append([name, f"[{lo:.2f}, {hi:.2f}]", pct(cov), pct(err), pct(called.mean()),
                 f"{np.median(lat):.0f}", f"{np.quantile(lat, .95):.0f}", f"{cost.mean() * 100000:.2f}"])
r = test[test.strategy == "rules"]
m = r.categoryProb == 1
rows.append(["Rules (matched only)", "-", pct(m.mean()), pct(1 - r[m].decisionOk.mean()), "0.0", "0", "0", "0.00"])
print(pd.DataFrame(rows, columns=["policy", "band", "automated %", "errors among automated %",
                                  "LLM called %", "p50 ms", "p95 ms", "$ per 100k"]).to_string(index=False))

if "jev-v2" in S:
    print("\n", "=" * 70, "\n11. BLOCK with criteria v2 plus targeted fixes for MISSING_DOCS (test split)\n")
    j = df[df.strategy == "jev-v2"].set_index(["id", "repeat"])
    l = df[df.strategy == "llm"].set_index(["id", "repeat"])
    t = j.split == "test"
    docs = j.category == "MISSING_DOCS"
    base = j.blockProb >= 0.5
    variants = [
        ("Jev v2 alone (0.5)", base, False),
        ("Jev v2 + WMS rule: docs block only if hazmat", pd.Series(np.where(docs, j.hazmat, base), index=j.index), False),
        ("Jev v2, MISSING_DOCS escalated to LLM", pd.Series(np.where(docs, l.blockProb >= THR["llm"], base), index=j.index), True),
    ]
    rows = []
    for name, pred, uses_llm in variants:
        lat = j.latencyMs + (np.where(docs, l.latencyMs, 0) if uses_llm else 0)
        rows.append([name, pct(((pred == j.trueBlock)[t]).mean()), pct(docs[t].mean() if uses_llm else 0),
                     f"{np.median(lat[t]):.0f}", f"{np.quantile(lat[t], .95):.0f}"])
    print(pd.DataFrame(rows, columns=["variant", "block acc %", "LLM called %", "p50 ms", "p95 ms"]).to_string(index=False))

# ---------------------------------------------------------------- charts
plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 11})
fig, ax = plt.subplots(figsize=(8, 4.2))
data = [df[df.strategy == s].latencyMs.clip(lower=0.01) for s in S]
ax.boxplot(data, orientation="horizontal", tick_labels=[{"rules": "Java rules", "jev": "Jev 1.13.0", "jev-v2": "Jev 1.13.0 (criteria v2)", "llm": "Claude Haiku 5.5"}[x] for x in S], showfliers=True,
           flierprops=dict(markersize=3, alpha=.4))
for i, d in enumerate(data, start=1):
    ax.annotate(f"p95 {d.quantile(.95):.1f} ms", (d.quantile(.95), i), xytext=(-20, 14),
                textcoords="offset points", fontsize=9)
ax.set_xscale("log"); ax.set_xlabel("Latency per decision (ms, log scale)")
ax.set_title(f"Latency per triage decision ({len(df) // len(S)} calls each)")
fig.tight_layout(); fig.savefig(f"{OUT}/fig5_latency.png", dpi=200)

fig, ax = plt.subplots(figsize=(5.8, 5.5))
ax.plot([0, 1], [0, 1], "--", color="gray", lw=1, label="Perfect calibration")
for s, name in [("jev", "Jev, block criteria v1"), ("jev-v2", "Jev, block criteria v2"), ("llm", "Claude Haiku 5.5 (verbalized)")]:
    if s not in S: continue
    x = test[test.strategy == s][["blockProb", "trueBlock"]].copy()
    x["b"] = pd.cut(x.blockProb, bins=np.linspace(0, 1.0001, 11), right=False)
    g = x.groupby("b", observed=True).agg(p=("blockProb", "mean"), a=("trueBlock", "mean"), n=("trueBlock", "size"))
    g = g[g.n >= 5]
    ax.plot(g.p, g.a, "o-", label=name)
ax.set_xlabel("Stated P(block)"); ax.set_ylabel("Share actually requiring a block")
ax.set_title("Block calibration (test split)"); ax.legend(loc="upper left")
ax.set_xlim(0, 1); ax.set_ylim(0, 1)
fig.tight_layout(); fig.savefig(f"{OUT}/fig_block_calibration.png", dpi=200)

fig, ax = plt.subplots(figsize=(7, 4.5))
for name in POLICIES[:3]:
    pts = sorted({stats(wt, name, lo, hi)[:2] for lo, hi in BANDS})
    front, best = [], -1   # coverage frontier: best coverage for each error level
    for cov, err in sorted(pts, key=lambda p: p[1]):
        if cov > best:
            front.append((err, cov)); best = cov
    ax.step([100 * e for e, c in front], [100 * c for e, c in front], where="post", label=name)
ax.axvline(100 * ERROR_BUDGET, color="gray", ls="--", lw=1)
ax.set_xlabel("Errors among automated decisions (%)"); ax.set_ylabel("Decisions automated (%)")
ax.set_xlim(0, 7); ax.set_ylim(70, 101)
ax.set_title("Best coverage per error level, all bands (test split)"); ax.legend(loc="lower right")
fig.tight_layout(); fig.savefig(f"{OUT}/fig_coverage.png", dpi=200)
print("\nCharts written.")