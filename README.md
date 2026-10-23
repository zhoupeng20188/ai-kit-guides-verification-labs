# AI Kit Guides verification labs

Runnable source projects and sanitized evidence for the engineering tutorials on [AI Kit Guides](https://aikitguides.com/).

These are synthetic teaching fixtures created with AI assistance and executed for the October 2026 editorial review. They are reproducible examples, not production incident reports, private customer data, or measurements of coding-agent performance.

## Projects

- [`boot4-verification-lab/`](boot4-verification-lab/) compares the same security, HTTP, and Flyway contracts on Spring Boot 3.5.7 and 4.0.0. Run `python3 verify.py` after installing JDK 17+, Maven, and Python 3.
- [`java-verification/`](java-verification/) demonstrates pricing rules, owner-scoped order access, ArchUnit checks, and test discovery. Run `./verify.sh` after installing JDK 17+ and Maven.

Each project contains its source, pinned build configuration, run instructions, and a sanitized evidence snapshot. The evidence preserves test identities, observations, counts, and expected negative cases while removing machine-specific JUnit properties and local account paths.

The fixtures use only in-memory or temporary databases and synthetic test users. They do not connect to a production service. The Boot project documents its loopback-only teaching credentials in its README; those credentials are intentionally non-secret and are not valid for any external system.

## Related pages

- [Boot migration walkthrough](https://aikitguides.com/blog/claude-code-spring-boot-4-migration-walkthrough/)
- [Boot verification evidence](https://aikitguides.com/examples/boot4-verification-lab/)

