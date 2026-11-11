# AI Kit Guides verification labs

Runnable source projects and sanitized evidence for the engineering tutorials on [AI Kit Guides](https://aikitguides.com/).

These are synthetic teaching fixtures created with AI assistance and executed for the October and November 2026 editorial reviews. They are reproducible examples, not production incident reports, private customer data, or measurements of coding-agent performance.

## Projects

- [`boot4-verification-lab/`](boot4-verification-lab/) compares the same security, HTTP, and Flyway contracts on Spring Boot 3.5.7 and 4.0.0. Run `python3 verify.py` after installing JDK 17+, Maven, and Python 3.
- [`java-verification/`](java-verification/) demonstrates pricing rules, owner-scoped order access, ArchUnit checks, and test discovery. Run `./verify.sh` after installing JDK 17+ and Maven.
- [`boot42-migrator-lab/`](boot42-migrator-lab/) exercises the Spring Boot 4.2.0-RC1 properties migrator `fail` switch across 18 startup cases: renamed and removed keys, misspelled keys, keys with no classpath consumer, `@PropertySource`, YAML and profile files. Run `bash verify.sh` after installing JDK 17+ and Maven 3.9+; a clean run reports 23 checks passed and 0 failed.
- [`boot42-redis-database-lab/`](boot42-redis-database-lab/) compares the configured Redis database with the database the auto-configured connection factory was built with, across 12 cases on Spring Boot 4.1.2 and 4.2.0-RC2 (standalone, master/replica, property and URL spellings). No Redis server is needed. Run `bash verify.sh` on JDK 17+; a clean run reports 44 checks passed and 0 failed.
- [`trustbut-junit6-lab/`](trustbut-junit6-lab/) runs trustbut 0.7.0's Maven runner against ten JUnit 6 changes: tests never discovered, skipped tests, no-op assertions, and `--strict`. Run `bash verify.sh` on JDK 17, Maven 3.9+ and Node 22+; a clean run reports 10 checks passed and 0 failed.

Each project contains its source, pinned build configuration, run instructions, and a sanitized evidence snapshot. The evidence preserves test identities, observations, counts, and expected negative cases while removing machine-specific JUnit properties and local account paths.

The fixtures use only in-memory or temporary databases and synthetic test users. They do not connect to a production service. The Boot project documents its loopback-only teaching credentials in its README; those credentials are intentionally non-secret and are not valid for any external system.

## Related pages

- [Boot migration walkthrough](https://aikitguides.com/blog/claude-code-spring-boot-4-migration-walkthrough/)
- [Boot verification evidence](https://aikitguides.com/examples/boot4-verification-lab/)
- [Spring Boot 4.2 properties migrator: four gaps that stay silent](https://aikitguides.com/blog/spring-boot-4-2-properties-migrator-silent-gaps/)
- [Spring Boot 4.2 Redis: why database=5 really meant db0](https://aikitguides.com/blog/spring-boot-4-2-redis-database-silent-fallback/)
