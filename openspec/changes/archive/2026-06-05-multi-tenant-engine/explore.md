## Exploration: Multi-tenant engine

### Current State
The app is a small layered Spring Boot service: `ReportController` handles two POST endpoints, `ReportOrchestrator` coordinates fill/export/print, `JasperFiller` loads compiled Jasper templates and prepares parameters, `QueryExecutor` runs SQL through Spring `JdbcTemplate`, and exporter components handle PDF/XLSX/DOCX/HTML. There is no real multi-tenant runtime model today: request auth is a single shared service token in a header, report templates are resolved from one shared filesystem directory, and datasource selection is effectively single-instance.

### Affected Areas
- `src/main/java/com/example/JaspertReport/controllers/ReportController.java` — shared token check and request entrypoint.
- `src/main/java/com/example/JaspertReport/services/ReportOrchestrator.java` — central flow for generation/printing.
- `src/main/java/com/example/JaspertReport/services/JasperFiller.java` — template resolution, compilation, and parameter wiring.
- `src/main/java/com/example/JaspertReport/services/QueryExecutor.java` — SQL execution via one `JdbcTemplate`.
- `src/main/resources/application.properties` — current config model and unused tenant-shaped properties.
- `src/main/resources/application.properties.example` — older datasource example still using `spring.datasource.*`.
- `src/main/resources/reportes/README.md` — documents the shared report directory.
- `pom.xml` — dependencies; no Spring Security is present.

### Approaches
1. **Central tenant registry + datasource routing** — keep one stateless app, resolve tenant from token/JWT, look up tenant config at request time, and route to the tenant datasource/template root/allowlist.
   - Pros: matches the target centralized model; one codebase serves many tenants; dedicated-instance mode can reuse the same config contract.
   - Cons: requires new auth/security layer and a tenant config source; more moving parts than the current single-datasource design.
   - Effort: High

2. **One deployment per tenant** — keep the current single-instance assumptions and deploy per tenant only.
   - Pros: minimal code change; simplest isolation story.
   - Cons: defeats the centralized-cost goal; poor operational scaling; doesn’t solve dynamic tenant resolution.
   - Effort: Low

### Recommendation
Take approach 1. The code already has the right service split for tenant-aware routing, but it needs a real tenant resolver, security, config binding, and per-tenant datasource/template abstraction. Dedicated instances should be a deployment mode, not a different code path.

### Risks
- `QueryExecutor` currently ignores the `datasource` field and executes against a single `JdbcTemplate`.
- `application.properties` defines `app.tenants.default.datasource.*`, but Spring Boot auto-config still expects `spring.datasource.*`; the tenant-shaped props are not wired.
- No Spring Security layer exists; auth is a raw header compare, so tenant identity and authorization are not trustworthy enough for multi-tenancy.
- Templates live on a shared filesystem path, so tenant isolation and per-tenant catalogs are not enforced.
- `./mvnw test` currently fails during compilation in this environment (`TypeTag :: UNKNOWN`), so build health needs attention before deeper changes.

### Ready for Proposal
Yes — the current state is clear enough to define a multi-tenant proposal with auth, datasource routing, template isolation, and deployment modes.
