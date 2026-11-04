# Spring Boot 4.2 properties migrator lab

A synthetic teaching application created with AI assistance for the October 30, 2026 article on the properties migrator. It is a new executed fixture, not a report of a production incident, and it measures one tool's behaviour rather than coding-agent performance.

The project answers a narrow question: when `spring.properties.migrator.fail` is set, which configuration problems stop the process and which ones continue to start it silently.

## Run the complete verification

Requirements: JDK 17+, Maven, and access to Maven repositories on the first run. No database, Docker, or network service is needed.

```bash
java -version
mvn -version
bash verify.sh
```

The script builds with `clean package`, then starts the same jar eighteen times — one per case, each in its own JVM — and compares the exit code and the presence of a migration report against the values recorded below. It exits 0 only when all 23 checks match.

Expected output ends with:

```
  checks passed: 23   failed: 0
  all cases match the recorded behaviour
```

## Version under test

| Component | Version |
| --- | --- |
| Spring Boot (parent, starters) | `4.2.0-RC1` |
| `spring-boot-properties-migrator` | `4.2.0-RC1`, `runtime` scope |
| JDK | Amazon Corretto `17.0.19` |
| Maven | `3.9.11` |

4.2 is not GA. The property exists in the 4.2 line only; there is no equivalent switch in 4.1.x. Re-run `verify.sh` against the GA you deploy before relying on these numbers, and change the parent version in `pom.xml` to do it.

Every case passes `--spring.main.web-application-type=none` in addition to its own arguments. Without it the application starts a server and never returns, so the exit code would reflect a killed process rather than a completed startup. The starters stay on the classpath, so the web configuration metadata is still present.

## Cases

Two key categories are used. `spring.resources.add-mappings` is a renamed key with a documented replacement. `spring.banner.image.width` is a key whose feature was removed.

| Case | Key source | `fail` | Exit | Report |
| --- | --- | --- | ---: | --- |
| `00-baseline` | none | default | 0 | no |
| `01-renamed-never` | command line | unset (`never`) | 0 | yes |
| `02-renamed-on-error` | command line | `on-error` | 0 | yes |
| `03-renamed-on-warning` | command line | `on-warning` | 1 | yes |
| `04-removed-never` | command line | unset (`never`) | 0 | yes |
| `05-removed-on-error` | command line | `on-error` | 1 | yes |
| `06-removed-on-warning` | command line | `on-warning` | 1 | yes |
| `07-invented-key` | command line, misspelled name | `on-warning` | 0 | no |
| `08-unbound-key` | command line, no consumer on classpath | `on-warning` | 0 | no |
| `09-propertysource` | `@PropertySource`, profile `psource` | `on-warning` | 0 | no |
| `10-profile-file` | `application-legacy.properties` | `on-warning` | 1 | yes |
| `11-config-import` | `spring.config.import` | `on-warning` | 1 | yes |
| `12-env-var-old-key` | `SPRING_RESOURCES_ADD_MAPPINGS` | `on-warning` | 1 | yes |
| `13-typo-fail-value` | — (`on-warn`) | invalid enum | 1 | no |
| `14-mixed-on-error` | command line, renamed + removed | `on-error` | 1 | yes |
| `15-yaml-old-key` | `fixtures/legacy.yml` via `spring.config.additional-location` | `on-warning` | 1 | yes |

Case 02 is the one worth reading twice: the value that sounds strictest lets a renamed key through, because the severity comes from how the metadata classifies the key, not from the consequence.

Case 14 shows the same thing from the other side. With both categories present, `on-error` does stop the process — because the removed key is error level. A single renamed key on its own does not.

Case 13 fails at binding, before any migration check runs. Its log carries the list of valid values instead of a migration report.

## Probes

Cases 16 and 17 print what the environment and the bound object actually hold, which is what separates "not reported" from "not working":

| Case | Old name | New name | Bound field |
| --- | --- | --- | --- |
| `16-probe-propertysource` | `false` | `null` | `true` (default) |
| `17-probe-commandline` | `false` | `false` | `false` |

The migrator performs the rename as well as the report. When it runs before the source is loaded, both are lost: the file keeps saying `false`, nothing is logged, and the bean keeps its default. `KeyProbe` and `ResourcesBindingProbe` produce those lines and are activated with `--probe=true`.

## Two traps found while building this

**A stale class output changes every result.** The first version of `verify.sh` ran `package` without `clean`. A leftover `application.yml` from an earlier experiment stayed in `target/classes`, was packaged into the jar, and made the baseline case report a renamed key. Every case after it was wrong in the same direction. The script now always runs `clean package`.

**A profile can load a second copy of the key you are trying to isolate.** Case 09 first activated a profile named `legacy`, which also matches `application-legacy.properties`. The run failed, but not because of `@PropertySource`. The profile for the annotated configuration class was renamed to `psource`, which has no matching profile file, and the case then produced a clean start. If you reproduce this against your own application, confirm that the activated profile does not supply the same key through config data.

## Evidence in this source package

`evidence/` holds the eighteen `.log` files produced by `verify.sh`: raw startup output for each case, with exit codes and the migration lines intact. Home and project paths and the local JDK path were replaced with placeholders; nothing else was edited.

`fixtures/legacy.yml` exists only for case 15. It is loaded through `--spring.config.additional-location`, which keeps it out of every other case; putting the same YAML at `src/main/resources/application.yml` would change every result in the table.

Running `verify.sh` deletes and recreates `verification-output/`, which is ignored by git, so the published snapshot stays inspectable and separate from a fresh run.

## Scope and limitations

- Release candidate only. No claim is made about 4.2.0 GA, 4.1.x, or later milestones.
- One JDK (17), one platform, one starter set (`spring-boot-starter-webmvc`). Other module combinations change which keys exist and which have consumers.
- Every case runs without a web server. Nothing here measures request handling, and nothing asserts runtime behaviour of `spring.web.resources` beyond the bound value printed by the probe.
- The fixture uses two synthetic legacy keys. It does not enumerate every renamed or removed key in 4.2, and the counts here say nothing about how many keys any real application uses.
- The switch is not evaluated for timing, throughput, or effect on startup performance.

Guided explanation: `/blog/spring-boot-4-2-properties-migrator-silent-gaps/` on AI Kit Guides.
