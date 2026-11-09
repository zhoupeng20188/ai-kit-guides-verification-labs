# Spring Boot 4.2 Redis database lab

A synthetic teaching fixture created with AI assistance for the November 9, 2026 article on the Redis database that a Boot 4 application actually connects to. It is a new executed fixture, not a report of a production incident. It measures Spring Boot's connection assembly, not coding-agent performance; no agent transcripts are included or quoted.

The project answers one narrow question: when `spring.data.redis.database` (or the database segment of `spring.data.redis.url`) is set alongside `spring.data.redis.masterreplica.nodes`, does the auto-configured connection factory carry that database, or does it fall back to `0`?

## Run the complete verification

Requirements: JDK 17+, Maven 3.9+, Python 3, and access to Maven repositories on the first run. **No Redis server, Docker, or network service is needed.**

```bash
export JAVA_HOME=/path/to/your/jdk-17
bash verify.sh
```

The script builds the same Java source twice — once against the `4.1.2` parent and once against `4.2.0-RC2` — then starts each jar twelve times in total, one case per fresh JVM. It compares the exit code, the probe's own assertion, and the reported factory type and database against the table below. It exits 0 only when every check matches.

Expected output ends with:

```
  checks passed: 44   failed: 0
  all cases match the recorded behaviour
```

Every run writes sanitized logs to `evidence/`. Machine-specific paths and timestamps are replaced with placeholders; nothing else is edited.

## Version under test

| Component | Version |
| --- | --- |
| Spring Boot (parent, starters) | `4.1.2` and `4.2.0-RC2` |
| `spring-boot-starter-data-redis` | managed by each parent |
| JDK | Amazon Corretto `17.0.19` |
| Maven | `3.9.11` |

`4.2.0-RC2` is a release candidate published on November 6, 2026; `4.2.0` GA was not yet available when this ran. `4.1.2` is the stable line, and is the realistic starting point for an upgrade. Re-run `verify.sh` against the GA you deploy before relying on these numbers.

## Behaviour table

`source` is what the configuration says. `factory` is what the auto-configured `LettuceConnectionFactory` was built with — the value its `RedisConfiguration` would use to select a database on the first real connection.

| Case | Boot | Configuration | Factory type | Factory DB |
| --- | --- | --- | --- | ---: |
| `01` | 4.1.2 | standalone, `database=5` | `RedisStandaloneConfiguration` | **5** |
| `02` | 4.1.2 | master/replica, `database=5` | `RedisStaticMasterReplicaConfiguration` | **0** |
| `03` | 4.1.2 | master/replica, `database=0` | `RedisStaticMasterReplicaConfiguration` | 0 |
| `04` | 4.1.2 | master/replica, `url=.../5` | `RedisStaticMasterReplicaConfiguration` | **0** |
| `05` | 4.1.2 | master/replica, `url=.../0` | `RedisStaticMasterReplicaConfiguration` | 0 |
| `06` | 4.1.2 | master/replica, no database | `RedisStaticMasterReplicaConfiguration` | 0 |
| `07` | 4.2.0-RC2 | standalone, `database=5` | `RedisStandaloneConfiguration` | **5** |
| `08` | 4.2.0-RC2 | master/replica, `database=5` | `RedisStaticMasterReplicaConfiguration` | **5** |
| `09` | 4.2.0-RC2 | master/replica, `database=0` | `RedisStaticMasterReplicaConfiguration` | 0 |
| `10` | 4.2.0-RC2 | master/replica, `url=.../5` | `RedisStaticMasterReplicaConfiguration` | **5** |
| `11` | 4.2.0-RC2 | master/replica, `url=.../0` | `RedisStaticMasterReplicaConfiguration` | 0 |
| `12` | 4.2.0-RC2 | master/replica, no database | `RedisStaticMasterReplicaConfiguration` | 0 |

Three observations follow directly from the table:

1. The standalone control returns `5` on **both** versions. The difference is not in the Redis client, the property binder, or the JDK; it is in the branch that assembles the master/replica configuration.
2. On `4.1.2`, case `02` binds `database=5` and still builds a factory with database `0`. The application starts with exit code 0 and no warning names the database. The two answers coexist silently.
3. Case `04` vs `10` covers the URL spelling of the same setting. The `url` does not switch the topology to standalone — the factory type stays `RedisStaticMasterReplicaConfiguration` — it only supplies the database that the `4.2.0-RC2` fix now applies.

## How the probe works

`RedisDatabaseProbe` injects the auto-configured `RedisConnectionFactory`, reads its `configuration` field reflectively (so the same source compiles against both parents), and calls `getDatabase()` on the resulting `RedisConfiguration`. Reflective access avoids depending on a method that only exists in one of the two versions.

`--spring.main.web-application-type=none` and `logging.level.root=WARN` keep the output to the probe lines. Each case passes `--probe.expect=<n>`; the probe throws if the factory disagrees, so a wrong result fails the JVM rather than only printing a different number.

### Two traps this fixture guards against

- **Stale configuration.** `application.properties` contains only the application name, banner, web type, and log level. An early version of this lab left `masterreplica.nodes` and `database=5` in that file; every case then reported `RedisStaticMasterReplicaConfiguration` with `source.property.database=5`, including the standalone controls. All twelve rows were wrong in the same direction. `clean package` plus command-line-only Redis properties is mandatory.
- **Lazy connections.** Lettuce does not open a socket during `afterPropertiesSet()`. A context that starts successfully is not evidence that Redis was reachable, nor that a `SELECT` was issued.

## What this does not prove

- It does not contact a Redis server, write a key, or run `CLIENT INFO`. It proves what the connection factory was **constructed with**, not what a server received.
- It does not measure cache-miss rates, data migration, replication lag, or any production behaviour.
- It does not compare coding agents, models, or prompts. No agent was run as part of this fixture.
- It covers one JDK, one client (Lettuce), one topology, and one pair of Boot versions. Jedis does not support master/replica connections; Sentinel and Cluster were not exercised.

For server-side confirmation, the upstream fix [spring-projects/spring-boot#52189](https://github.com/spring-projects/spring-boot/pull/52189) records a real Redis 8.6.2 master/replica check: with `database=1`, a key written through the auto-configured `StringRedisTemplate` landed in database 0 before the fix and database 1 afterwards.

## Official references

- [Spring Boot 4.2.0-RC2 release announcement](https://spring.io/blog/2026/11/06/spring-boot-4-2-0-RC2-available-now)
- [Spring Boot 4.2.0-RC2 Release Notes — Redis Database Applied to Master/Replica Connections](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.2.0-RC2-Release-Notes)
- [Spring Boot 4.2 reference — Connecting to Redis](https://docs.spring.io/spring-boot/4.2/reference/data/nosql.html)
- [Redis `CLIENT INFO`](https://redis.io/docs/latest/commands/client-info/)

## Related pages

- [Spring Boot 4 YAML: verify sources, binding, and behaviour](https://aikitguides.com/blog/codex-spring-boot-4-yml-properties/)
- [Spring Boot 4.2 Properties Migrator: four gaps that stay silent](https://aikitguides.com/blog/spring-boot-4-2-properties-migrator-silent-gaps/)
- [Spring Boot 4 dependencies: resolved does not mean configured](https://aikitguides.com/blog/codex-spring-boot-4-dependencies-silent-failures/)
