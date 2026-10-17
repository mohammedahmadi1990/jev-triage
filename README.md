# jev-triage

**Do we really need an LLM for every decision?** This repository benchmarks three ways to triage warehouse goods-receipt remarks such as *"3 Kartons beschädigt, 2 Artikel fehlen. Bitte prüfen."*:

- **Java rules:** a keyword rule engine, no AI
- **[Jev](https://docs.typesafe.ai)** (TypeSafe AI): a decision model that returns typed answers with probabilities instead of text
- **Claude Haiku 5.5** via Spring AI: a general-purpose LLM with structured output

Each approach answers the same three questions for every remark: the **category** of the problem, whether the goods must be **blocked** from putaway, and the **severity**.

This is the companion code for the article *[Do We Really Need an LLM for Every Decision? Testing Jev with Spring Boot](#)* by Mohammed Ahmadi.

## Key results

Test split (100 remarks × 3 repeats), labels v2. Full output: [`analysis/final/analysis-output.txt`](analysis/final/analysis-output.txt).

| | Java rules | Jev 1.13.0 | Jev, criteria v2 | Claude Haiku 5.5 |
| --- | --- | --- | --- | --- |
| Category accuracy | 41.0% | 100% | 100% | 100% |
| Block accuracy | 66.0% | 72.0% | 85.0% | 98.0% |
| Severity accuracy | 48.0% | 73.3% | 72.3% | 77.3% |
| Latency p50 / p95 | < 0.1 ms | 229 / 283 ms | 225 / 280 ms | 1,523 / 2,399 ms |
| Cost per 100k decisions | $0 | $2.26 | $2.49 | $20.31 |

What the numbers say:

- **Categorization is solved** for both models on this dataset. The rules match only 36% of remarks.
- **The block decision is where they differ.** Rewording Jev's block criteria raised its accuracy from 72% to 85%. Combining Jev with one deterministic rule from master data (missing documents block only for hazardous goods) reached 97.0%, close to the LLM, at Jev's latency and without any LLM call. See section 11 of the analysis output.
- **Jev is about 6–8× faster and about 9× cheaper** than Claude Haiku 5.5, a much smaller cost gap than often claimed, because Haiku 5.5 is inexpensive.

Please read the [limitations](#limitations) before reusing these numbers.

## How it works

All approaches implement one interface, so the benchmark treats them identically:

```java
public interface TriageStrategy {
    String name();
    Prediction triage(String remark);
}
```

- **Shared criteria:** Jev and the LLM receive the same instructions and option descriptions, defined once in `TriageCriteria` and the `Category` / `Severity` enums.
- **Type-safe end to end:** Jev's option keys and criteria are generated from Java enums, and responses are mapped back to the same enums.
- **Interleaved runs:** for each remark, all strategies run back to back, so network and time-of-day effects hit all of them equally.
- **Failures are data:** exceptions are recorded as `FORMAT_FAILURE` or `ERROR` instead of stopping the run.
- **Hybrid policies are replayed, not re-run:** every remark has both a Jev and an LLM result, so policies such as "Jev first, LLM when unsure" are evaluated from the recorded results in `analyze.py`.

## Project structure

```
src/main/java/org/ahmadi/jevtriage/
├── config/       RestClient and properties for the Jev API
├── domain/       enums, records, shared criteria, dataset loader
├── jev/          JevClient, request builders, response records
├── llm/          Spring AI client for Claude Haiku 5.5
├── rules/        keyword rule baseline
└── benchmark/    strategies and the benchmark runner
src/main/resources/
├── application.yaml
├── application-smoke.yaml       smoke tests, no web server
├── application-benchmark.yaml   benchmark runs, no web server
└── data/
    ├── remarks.tsv              200 labeled synthetic remarks
    └── Labeling.md              labeling rules and revision log
results/                         raw benchmark output (TSV)
analysis/
├── analyze.py                   reproduces all tables and charts
├── requirements.txt
└── final/                       analysis output and charts
```

## Requirements

- Java 21
- Gradle (wrapper included)
- A TypeSafe API key for Jev, and an Anthropic API key with credits. A Claude app subscription does not include API access.
- Python 3.12+ for the analysis only

Built with Spring Boot 4.1 and Spring AI 2.0.

## Setup

Create a `.env` file in the project root. It is read by Spring and ignored by Git:

```
TYPESAFE_API_KEY=your-jev-key
ANTHROPIC_API_KEY=your-anthropic-key
```

Never commit this file. GitHub's push protection blocked a key in this very repository during development.

## Running

**Smoke tests:** a few calls per approach, printed to the console:

```bash
./gradlew bootRun --args='--spring.profiles.active=smoke'
```

**Dry run of the benchmark:** 5 remarks, 1 repeat, a few seconds:

```bash
./gradlew bootRun --args='--spring.profiles.active=benchmark --benchmark.limit=5 --benchmark.repeats=1'
```

**Full benchmark:** 200 remarks × 3 repeats per strategy. With all strategies, this takes about 20 minutes and costs well under $1:

```bash
./gradlew bootRun --args='--spring.profiles.active=benchmark'
```

**Selected strategies only:**

```bash
./gradlew bootRun --args='--spring.profiles.active=benchmark --benchmark.strategies=jev-v2'
```

| Property | Default | Meaning |
| --- | --- | --- |
| `benchmark.repeats` | `3` | Runs per remark and strategy |
| `benchmark.limit` | `0` (all) | Only the first N remarks |
| `benchmark.strategies` | all | Comma-separated: `rules`, `jev`, `jev-v2`, `llm` |

Results are written to `results/benchmark-<timestamp>.tsv`, one row per call, with the true labels next to each prediction.

## Reproducing the analysis

```bash
python -m venv .venv
source .venv/bin/activate          # Windows: .\.venv\Scripts\Activate.ps1
pip install -r analysis/requirements.txt

python analysis/analyze.py \
  results/benchmark-20261017-202326.tsv \
  results/benchmark-20261017-211241.tsv \
  --labels src/main/resources/data/remarks.tsv \
  --out analysis/final
```

`--labels` applies the current labels (v2). Leave it out to use the labels recorded at run time (v1).

The script reports accuracy, latency, cost, calibration, repeat variance, results by language and tag, tuned block thresholds, severity confusion matrices, safe-automation policies, and the targeted block variants. It also draws three charts.

## Dataset

[`remarks.tsv`](src/main/resources/data/remarks.tsv) holds 200 synthetic goods-receipt remarks:

- 113 German, 82 English, 5 mixed
- About 40 per category, stratified 50/50 into a calibration split (used to choose thresholds) and a test split (used to report results)
- About 60% non-trivial cases: indirect descriptions, abbreviations, negations, several problems in one remark, typos

The remarks are synthetic and contain no customer or company data. Labels were drafted with AI assistance and reviewed by hand. The rules, the known biases, and every label change after the first run are documented in [`Labeling.md`](src/main/resources/data/Labeling.md).

## Limitations

- **Small dataset.** With 100 test remarks, one remark is about one percentage point. Treat differences of a few points as noise.
- **Synthetic data.** Real remarks are messier. The hazmat flag in particular correlates with the labels more cleanly than it would in a real warehouse master data system.
- **Post-hoc changes.** The criteria v2 wording and the hazmat rule for missing documents were designed after seeing the errors of the first run. Their test results are therefore less independent than the others.
- **Label review.** Two labels were changed after the first run, both in the direction of the models' answers. Results are reported for both label versions, and no conclusion changed.
- **Rule bias.** The dataset was written after the rule baseline. Rows tagged `keyword_trap` contain rule keywords in a harmless context.
- **Snapshot.** Models, prices, and APIs change. Results reflect Jev 1.13.0 and Claude Haiku 5.5 in October 2026.

## License

[MIT](LICENSE) © Mohammed Ahmadi