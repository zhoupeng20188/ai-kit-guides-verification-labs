# Spring Boot 3.5 to 4 verification lab

A synthetic teaching application created with AI assistance for the October 23, 2026 editorial review. It is a new executed fixture, not a recovered historical experiment or a report of a production incident.

The same source contracts run against two Maven profiles:

| Profile | Spring Boot | Integration dependencies |
| --- | --- | --- |
| `boot3` (default) | 3.5.7 | `spring-boot-starter-web`, `flyway-core` |
| `boot4` | 4.0.0 | `spring-boot-starter-webmvc`, `spring-boot-starter-flyway` |

The Boot BOM manages library versions. Compiler, Surefire, dependency and Boot Maven plugin versions are explicit. These historical versions support a repeatable comparison; the project does not select a current production release for you.

## Run the complete verification

Requirements: JDK 17+, Maven, Python 3, and access to Maven repositories on the first run. No Docker or external database is needed. Check that Maven uses the intended JDK:

```bash
java -version
mvn -version
python3 verify.py
```

The script runs the commands below, captures raw logs under ignored `verification-output/`, and checks named reports and counts. It returns success only when both normal profiles pass and both fault demonstrations fail for the expected assertions.

```bash
mvn -B -Pboot3 clean test
mvn -B -Pboot4 clean test
mvn -B -Pboot4 test -Dtest=UndiscoveredShould
mvn -B -Pboot4 test -Dtest=NetworkContractTest \
  -Dlab.allow-error-dispatch=false \
  -Dlogging.level.org.springframework.security=TRACE
```

Expected: 11 passing tests in each normal profile; one failing test and exit 1 in each of the last two commands. The verification script itself exits 0 when it confirms those outcomes. A dependency-download or compilation failure does not satisfy either negative demonstration.

## Inspect the checks

| Source | Cases | What it establishes |
| --- | ---: | --- |
| `SecurityContractTest.java` | 6 | Authenticated GET 200, anonymous GET 401, missing/invalid CSRF 403, valid CSRF 200, and an unsecured standalone harness returning 200 |
| `NetworkContractTest.java` | 1 | Loopback HTTP Basic requests: anonymous GET 401, authenticated GET 200, authenticated POST without a CSRF token 403 |
| `DatabaseActivationTest.java` | 1 | Boot creates a Flyway bean and applies the classpath migration at startup |
| `FlywayHistoryTest.java` | 3 | Edited migration fails validation; repair leaves old/fresh schemas different; restoring history plus V3 aligns the checked schema/data |
| `UndiscoveredShould.java` | 1 outside defaults | Explicit selection exposes `DISCOVERY_SENTINEL`; a normal run does not discover this class |

`OBSERVATION` lines in the Maven logs report actual response statuses and queried schema values. The tests also assert those observations. Database migration files for the history exercise are written into a temporary directory by `FlywayHistoryTest`; each case gets its own disposable H2 databases. No command targets a shared database.

### CSRF exercise

The real test setup uses `webAppContextSetup(context).apply(springSecurity())`. The intentionally incomplete setup uses `standaloneSetup(new NotesController())` without security filters. Comparing them exposes why the same 200 status can provide different evidence.

Authentication for most tests is a synthetic Spring Security test user. This does not exercise a real HTTP Basic credential exchange. CSRF protection was already present before Boot 4; consistent rejection in both profiles is the expected contract.

`NetworkContractTest` separately uses real HTTP Basic over a loopback connection. The normal configuration permits the servlet's internal `ERROR` dispatcher type so an error response can be rendered, while direct requests to `/error` still require authentication. Disabling this narrow permission with `lab.allow-error-dispatch=false` reproduces a 401 that masks the CSRF filter's original 403. The fourth command keeps the expected 403 assertion and therefore fails. Its trace records the initial CSRF denial and subsequent error authorization. CSRF protection remains enabled in both configurations.

### Flyway exercise

V1 creates a table and an existing row. Original V2 adds nullable `tag`. The edited V2 adds a default and a non-null constraint instead. An existing database rejects its changed checksum; a fresh database executes it. Repair can align history metadata while the existing column remains nullable and the fresh column is not.

The corrective path restores original V2, checks the restored applied history, then adds V3 to backfill existing rows and set the default/constraint. It migrates and validates both the old and fresh databases. A standalone validation after adding a pending V3 can itself report an unapplied migration; the test deliberately checks restored history before creating V3 and validates again after migration.

## Evidence in this source package

`evidence/results.json` records execution time, resolved versions, command statuses, test totals and SHA-256 digests for the POM and source files. `evidence/boot3/`, `boot4/`, `discovery/` and `error-dispatch/` contain JUnit XML. Corresponding `.log` files retain the Maven output.

Home/project paths and the local account name were redacted, JUnit system properties were removed, and trailing whitespace in published Maven logs was normalized. The test names, assertions, failure and observed values remain inspectable. Running `verify.py` creates a new evidence set under `verification-output/evidence/`, preserving this published snapshot.

## Application scope and limitations

The application is an in-memory notes counter used to exercise HTTP handling, plus a separate migration marker table used to prove startup integration. It is not a complete notes product. MockMvc does not open a network socket; `NetworkContractTest` separately uses a loopback HTTP server on a random port. H2 does not establish PostgreSQL/MySQL SQL semantics, locking, performance, or production upgrade safety. These eleven checks do not exhaust Boot 4 compatibility or compare coding-agent performance.

For optional local inspection, `mvn -Pboot4 spring-boot:run` binds to `127.0.0.1`. The public fixture credentials are `lab-user` / `lab-only`. The database and counter are ephemeral; the fixture is not intended as a deployed service.

Guided explanation: `/blog/claude-code-spring-boot-4-migration-walkthrough/` on AI Kit Guides. Source and evidence landing page: `/examples/boot4-verification-lab/`.
