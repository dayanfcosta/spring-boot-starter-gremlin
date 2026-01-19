# Project Roadmap

Future improvements and enhancements for the Spring Boot Starter Gremlin project.

## CI Pipeline Enhancements

- [ ] Code coverage (JaCoCo + Codecov/Coveralls)
- [ ] Branch protection rules (require CI to pass before merging)
- [ ] PR comments with build status/test summary
- [ ] Dependency vulnerability scanning
- [ ] Ktlint/Detekt for Kotlin static analysis

## Developer Experience (DX) Improvements

### High Impact

- [ ] **Micrometer Metrics Integration** - Expose query metrics to Spring Boot Actuator (query count, duration histograms, connection pool stats, error rates)
- [ ] **Repository Abstraction** - Spring Data-like repository pattern for Gremlin entities
- [ ] **Configuration Validation** - Fail fast with clear error messages at startup (validate connectivity, credentials, serializer compatibility)

### Medium Impact

- [ ] **Kotlin DSL for Queries** - More idiomatic Kotlin API for Gremlin traversals
- [ ] **Sample Projects** - Working examples for Neptune, Cosmos DB, multi-tenant setups

### Lower Priority

- [ ] **Reactive/WebFlux Support** - Coroutines + Flow integration
- [ ] **Schema Validation** - Validate graph schema at startup
- [ ] **Documentation Files** - Create the docs referenced in README (configuration-modes.md, configuration-reference.md, examples, advanced topics)
