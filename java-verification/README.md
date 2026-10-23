# Java verification teaching project

This small standalone project was created for the October 2026 content revision. It does not reconstruct earlier AI-agent trials or implement a complete Spring Boot service.

It demonstrates:

- Pricing: inclusive 100.00 threshold, 10% discount rounded HALF_UP to two decimal places, and invalid-input rejection.
- Order lookup: trusted owner identity scopes access; another owner and an absent identity do not retrieve an order. There is no HTTP authentication or database here.
- ArchUnit: a rule rejects a deliberately annotated fixture and accepts a clean fixture. The rejected check is caught by a JUnit assertion, so the normal suite passes.
- Discovery: `UndiscoveredShould` deliberately fails but is outside Surefire's default test-class naming patterns. Explicit selection discovers it.

## Requirements and commands

Use JDK 17 or newer and Maven. Dependencies are pinned in `pom.xml`; the first build needs access to their repositories. Ensure `mvn -version` reports the intended JDK.

```bash
mvn -B clean test
# Expected: 9 tests, no failures/errors/skips, exit 0.
mvn -B test -Dtest=UndiscoveredShould
# Expected: 1 deliberately failing test, exit 1.
```

The second command is an intentional negative demonstration. Its failure is the expected result, not a defect to fix by deleting the assertion. `failIfNoTests` guards against an empty overall selection; it does not prove that every intended class was discovered.

`./verify.sh` captures both results and checks the expected report counts. It runs only these Maven commands. Reports and logs are written under `verification-output/`; generated files are excluded from the download.

## Observed run, October 23, 2026

Environment: macOS, Amazon Corretto OpenJDK 17.0.19, Apache Maven 3.9.11.

| Command | Tests | Failures | Errors | Skipped | Exit |
| --- | ---: | ---: | ---: | ---: | ---: |
| `mvn -B clean test` | 9 | 0 | 0 | 0 | 0 |
| `mvn -B test -Dtest=UndiscoveredShould` | 1 | 1 | 0 | 0 | 1 |

The full run logs were retained locally during the content audit. These results establish the behavior of this source and configuration in that environment. They do not measure agent performance, application authorization, Spring Boot migration compatibility, or Gradle behavior.

The downloadable ZIP also includes JUnit XML records under `verification/`. Machine-specific system properties and captured console streams were removed; test identities, counts, and the intentional failure remain inspectable.
