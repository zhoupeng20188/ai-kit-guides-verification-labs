# trustbut-junit6-lab

What does a verification receipt actually prove about tests written in a Maven / JUnit 6 project?

This lab runs [trustbut](https://github.com/isAlmogK/trustbut) 0.7.0 against ten small changes to one Java project. Each case is a single commit on top of the same base revision. The business rule is deliberately tiny (`Price.qualifies`, an off-by-one at 10000) so that the only variable is whether a test ran and whether its assertion constrains anything.

This is a synthetic teaching fixture. It measures the behaviour of one verification tool on one Maven project. It is **not** a measurement of any coding agent's performance, and it says nothing about trustbut's other runners or languages.

## Run it

Requirements: JDK 17, Maven 3.9+, Node.js 22+, git, and trustbut 0.7.0.

```sh
npm install --prefix /tmp/trustbut-cli trustbut@0.7.0
JAVA_HOME=/path/to/jdk-17 \
TRUSTBUT_BIN=/tmp/trustbut-cli/node_modules/.bin/trustbut \
bash verify.sh
```

`TRUSTBUT_BIN` is optional; without it the script uses `npx --yes trustbut@0.7.0`. `JAVA_HOME` is required because the script pins the toolchain — on the author's machine `mvn` otherwise picks a different JDK than `java`.

The script creates a temporary git repository, so it never modifies this repository's history. Evidence is written to `evidence/`: `summary.tsv`, one `.receipt.md` and `.receipt.json` per case, and the Maven summary lines per case. A clean run prints `checks passed: 10 failed: 0`.

Pinned versions used for the recorded evidence: Amazon Corretto 17.0.19, Maven 3.9.11, JUnit Jupiter 6.0.3, Surefire 3.5.6, Node 22.22.2, trustbut 0.7.0.

## Results

| # | Case | Change | `mvn -B test` | Receipt | exit |
|---:|---|---|---|---|---:|
| 01 | control-red | real boundary assertion in `PriceTest` | FAIL | **FAIL** | 1 |
| 02 | proved | fix `>=` plus real boundary assertion | pass | **PASS**, "1 proves the change" | 0 |
| 03 | should | new `PriceShould.java` (not discovered) | pass | **PASS**, Findings None | 0 |
| 04 | it | new `PriceIT.java` (not discovered by Surefire) | pass | **PASS**, Findings None | 0 |
| 05 | disabled | new `@Disabled` test | pass, 1 skipped | **WARN**, "Test skipped" | 0 |
| 06 | disabled-strict | case 05 with `--strict` | pass, 1 skipped | **WARN** | 1 |
| 07 | weakened | new test with `assertTrue(true)` | pass | **PASS**, "1 pass without it", Findings None | 0 |
| 08 | code-only | fix `>=`, no test added or changed | pass | **WARN**, "nothing proves this change" | 0 |
| 09 | weakened-strict | case 07 with `--strict` | pass | **PASS** | 0 |
| 10 | should-strict | case 03 with `--strict` | pass | **PASS** | 0 |

## What the receipts show

- **A test that never ran is invisible.** Cases 03 and 04 add a test whose name Surefire's default includes do not match. The receipt is `PASS` with `Findings: None`; it has no "New/changed tests" section at all. The underlying off-by-one is still present.
- **A test that ran but proves nothing is quieter than no test at all.** Case 07's receipt explicitly records `New/changed tests: 0 prove the change · 1 pass without it`, yet raises no finding and returns `PASS` / exit 0. Case 08, which changes production code and adds no test, gets a `nothing proves this change` WARN. Adding a no-op test suppresses that warning.
- **`--strict` only changes WARN.** It turns case 05's exit code from 0 into 1. It does not turn a `PASS` into a failure: cases 09 and 10 still exit 0. A CI job that gates on the exit code alone accepts cases 03, 04, 07, 09 and 10.

Cases 01 and 02 are the forward controls. The Maven runner does detect a genuinely failing test and does credit a fail-before / pass-after proof. These are detection boundaries under the default Surefire configuration, not a broken install.

## Limits

- The `pom.xml` intentionally keeps Surefire's **default** configuration: no `failIfNoTests`, no Failsafe binding, no custom includes. A project that adds those gates changes the outcome.
- Only the `maven` runner of trustbut 0.7.0 was exercised. The tool's spec documents that Maven and Gradle reports name classes rather than source files; this lab does not isolate that mechanism from other possible causes.
- Receipt behaviour can change between tool versions. Re-run against the version you actually deploy.
