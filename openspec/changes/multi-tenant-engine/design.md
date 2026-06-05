# Design: Multi-Tenant Engine

## Technical Approach

Introduce a `HandlerInterceptor` that resolves `X-Service-Token` to a tenant identity before each controller method. A thread-local `TenantContext` carries the identity through the request. A configuration-backed `TenantProperties` loads all tenant definitions (datasource, template root, allowlist) at startup. `DataSourceManager` creates one HikariCP pool per tenant and validates them on init.

**SOLID by design**: Every cross-cutting concern is behind an interface so concrete implementations can evolve independently. `TenantResolver`, `ReportAllowlistService`, `TemplateResolver`, `DataSourceProvider`, and `ReportCompiler` are all injected as constructor dependencies — existing services depend on abstractions, never on concretions. The interceptor's token validation is split from tenant context initialization (SRP). The same binary supports `centralized` and `dedicated` deployment profiles via a single config property.

## Architecture Decisions

| # | Decision | Options Considered | Choice | Rationale |
|---|----------|-------------------|--------|-----------|
| 1 | Token resolution entry point | Filter, HandlerInterceptor, AOP | **HandlerInterceptor** | Runs inside Spring context (bean injection available); `preHandle` short-circuits before controller; `afterCompletion` guarantees ThreadLocal cleanup. Filter lacks Spring DI; AOP is overkill for header-based auth. |
| 2 | Tenant context carrier | Request-scoped bean, ThreadLocal, method parameters | **ThreadLocal** (`TenantContext`) | Stateless, zero-allocation per request, already idiomatic for Spring security context. Cleared in `afterCompletion`. |
| 3 | Datasource routing | `AbstractRoutingDataSource`, HikariCP per tenant | **HikariCP per tenant** (`DataSourceManager`) | `AbstractRoutingDataSource` shares a single pool — one noisy tenant starves others. Per-tenant pools give full isolation, independent config tuning, and clear startup validation. |
| 4 | Template compilation | Startup `@PostConstruct`, lazy on first request | **Lazy on first request** | Startup compilation with N tenants blocks deployment. Lazy compilation (check `.jrxml` vs `.jasper` timestamp, compile if needed) matches existing `fill()` pattern and scales. |
| 5 | Allowlist enforcement point | Controller, Interceptor, Orchestrator | **Orchestrator** | Controller is thin HTTP mapping. Interceptor shouldn't inspect request body. `ReportOrchestrator.generate()` validates report name against tenant allowlist before filling — clean separation. |
| 6 | Deployment profile | Two Spring profiles, single config property | **Single config property** (`app.profile`) | Two profiles mean two code paths — violates "same binary" guarantee. One property read at interceptor resolution is simpler: `dedicated` mode rejects non-assigned tenants with 403. |
| 7 | SOLID compliance | Direct concrete dependencies vs interfaces | **Interface-first with constructor injection** | SRP: split `TenantInterceptor` into `TokenValidator` + `TenantContextInitializer`. OCP: every strategy (tenant resolution, allowlist, templates, datasource, compilation) behind its own interface — new tenant sources or validation rules never touch existing code. DIP: `ReportOrchestrator`, `JasperFiller`, and `QueryExecutor` receive interfaces via constructor injection, never concrete classes. Mocking trivial for testing. |

## SOLID Principles Applied

| Principle | Abstraction | Implementation | Where Enforced |
|-----------|-------------|----------------|----------------|
| **S**RP | `TokenValidator` + `TenantContextInitializer` | Split from monolithic `TenantInterceptor` | `WebConfig` wires both as separate interceptors |
| **O**CP | `TenantResolver` | `ConfigBasedTenantResolver` | New tenant sources (DB, LDAP) add impls without touching resolve logic |
| **O**CP | `ReportAllowlistService` | `ConfigBasedAllowlistService` | New allowlist rules (time-based, quota-based) are new impls |
| **O**CP | `TemplateResolver` | `TenantScopedTemplateResolver` | Template sources (filesystem, S3, classpath) add impls |
| **O**CP | `ReportCompiler` | `LazyReportCompiler` | Compilation strategies (pre-compile, cache-only) are new impls |
| **L**SP | `DataSourceProvider` | `DataSourceManager` | `QueryExecutor` depends on `DataSourceProvider`; any compliant impl (mock, pool-per-tenant, single-pool) works |
| **I**SP | Interfaces are narrow | 5 focused interfaces | No "god interface" — each consumer sees only what it needs |
| **D**IP | Constructor injection throughout | `ReportOrchestrator(ReportAllowlistService)`, `JasperFiller(TemplateResolver, ReportCompiler)`, `QueryExecutor(DataSourceProvider)` | Zero `new` of concrete classes in services; Spring DI wires implementations |

## Data Flow

```
HTTP POST /reportes/generar  (X-Service-Token, ReportRequestDTO)
         │
         ▼
┌─────────────────────────┐
│ TokenValidator          │  preHandle: header present? valid format?
│ (HandlerInterceptor)    │  delegates to TenantResolver.validate(token)
└────────────┬────────────┘
         │  401 if missing/invalid
         ▼
┌─────────────────────────┐
│ TenantContextInit       │  preHandle: TenantResolver.resolve(token) → Tenant
│ (HandlerInterceptor)    │  dedicated mode? → enforce assigned tenant
│                         │  TenantContext.set(tenant)
└────────────┬────────────┘
         │  403 if wrong dedicated tenant
         ▼
┌─────────────────────────┐
│ ReportController        │  validates request body (existing logic)
└────────────┬────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│ ReportOrchestrator                   │  allowlistService.isAllowed(tenantId, reportName)
│ .generate(request)                   │  403 if report not in allowlist
│  depends on: ReportAllowlistService  │
└────────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│ JasperFiller.fill()                  │  templateResolver.resolve(tenantId, reportName) → .jasper path
│  depends on: TemplateResolver        │  reportCompiler.compileIfNeeded(jrxml)
│  depends on: ReportCompiler          │  SUBREPORT_DIR = tenant.reportesRuta
│                                      │  buildParams → queryExecutor.execute(sql)
└────────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────────┐
│ QueryExecutor                        │  dataSourceProvider.getTemplate(tenantId)
│ .execute(sql)                        │  → template.queryForList(sql)
│  depends on: DataSourceProvider      │
└────────────┬─────────────────────────┘
         │
         ▼
┌─────────────────────────┐
│ ExporterRegistry        │  format → ReportExporter.export(jasperPrint)
└────────────┬────────────┘
         │
         ▼
  Response: 200 + bytes + content-type header

TenantContextInit.afterCompletion → TenantContext.clear()
```

Interfaces shown in **bold** are constructor-injected by Spring. Every service depends on an abstraction, never on a concrete class.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/.../config/TenantProperties.java` | **Create** | `@ConfigurationProperties("app")` — binds profile, assignedTenant, tenants map |
| `src/main/java/.../config/WebConfig.java` | **Create** | Registers `TokenValidator` and `TenantContextInitializer` interceptors on `/reportes/**` |
| `src/main/java/.../tenant/Tenant.java` | **Create** | Value record: id, reportesRuta, allowedReports, allowedFormats, datasource config |
| `src/main/java/.../tenant/TenantContext.java` | **Create** | `ThreadLocal<Tenant>` holder with `getCurrentTenant()`, `set()`, `clear()` |
| `src/main/java/.../tenant/TenantResolver.java` | **Create** | **Interface**: `resolve(token) → Tenant`, `validate(token) → boolean` |
| `src/main/java/.../tenant/ConfigBasedTenantResolver.java` | **Create** | Impl: reads `TenantProperties.tenants` map, matches token → tenant |
| `src/main/java/.../tenant/TokenValidator.java` | **Create** | `HandlerInterceptor` — validates header presence/format, delegates to `TenantResolver.validate()` |
| `src/main/java/.../tenant/TenantContextInitializer.java` | **Create** | `HandlerInterceptor` — resolves tenant via `TenantResolver`, enforces dedicated mode, populates `TenantContext` |
| `src/main/java/.../tenant/DataSourceProvider.java` | **Create** | **Interface**: `getTemplate(tenantId) → JdbcTemplate` |
| `src/main/java/.../tenant/DataSourceManager.java` | **Create** | Impl: creates HikariCP per tenant, validates at startup, implements `DataSourceProvider` |
| `src/main/java/.../services/ReportAllowlistService.java` | **Create** | **Interface**: `isAllowed(tenantId, reportName) → boolean` |
| `src/main/java/.../services/ConfigBasedAllowlistService.java` | **Create** | Impl: reads allowlist from `Tenant` record |
| `src/main/java/.../services/TemplateResolver.java` | **Create** | **Interface**: `resolve(tenantId, reportName) → Path` |
| `src/main/java/.../services/TenantScopedTemplateResolver.java` | **Create** | Impl: resolves `.jasper` from `tenant.reportesRuta/reportName.jasper` |
| `src/main/java/.../services/ReportCompiler.java` | **Create** | **Interface**: `compileIfNeeded(jrxmlPath) → jasperPath` |
| `src/main/java/.../services/LazyReportCompiler.java` | **Create** | Impl: checks `.jrxml` vs `.jasper` timestamp, compiles if stale |
| `src/main/java/.../exceptions/TenantResolutionException.java` | **Create** | Unchecked exception → 401 |
| `src/main/java/.../exceptions/ReportNotAllowedException.java` | **Create** | Unchecked exception → 403 |
| `src/main/java/.../controllers/ReportController.java` | **Modify** | Remove `@Value serviceToken` + inline token check; auth handled by interceptors |
| `src/main/java/.../services/ReportOrchestrator.java` | **Modify** | Constructor-inject `ReportAllowlistService`; enforce before fill |
| `src/main/java/.../services/JasperFiller.java` | **Modify** | Remove `@PostConstruct compileReports()` and `@Value app.reportes.ruta`; constructor-inject `TemplateResolver` + `ReportCompiler`; lazy per-request compilation |
| `src/main/java/.../services/QueryExecutor.java` | **Modify** | Constructor-inject `DataSourceProvider` instead of `JdbcTemplate`; resolve tenant template at call time |
| `src/main/java/.../exceptions/GlobalExceptionHandler.java` | **Modify** | Add handlers for `TenantResolutionException` (401) and `ReportNotAllowedException` (403) |
| `src/main/resources/application.properties` | **Modify** | Replace single-tenant props with `app.profile`, `app.assigned-tenant`, `app.tenants.*` |
| `src/main/resources/application.properties.example` | **Modify** | Document new tenant properties structure |
| `src/test/java/.../*.java` | **Create** | Unit tests for all interfaces/impls; integration tests for interceptor chain and E2E flow |

## Configuration Structure

```properties
# Deployment mode: centralized | dedicated
app.profile=${APP_PROFILE:centralized}
app.assigned-tenant=    # only for dedicated mode

# Per-tenant definitions (repeat for each tenant)
app.tenants.acme.service-token=${ACME_TOKEN}
app.tenants.acme.reportes-ruta=${ACME_REPORTS_PATH:/reportes/acme/}
app.tenants.acme.datasource.url=${ACME_DB_URL}
app.tenants.acme.datasource.username=${ACME_DB_USER}
app.tenants.acme.datasource.password=${ACME_DB_PASSWORD}
app.tenants.acme.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
app.tenants.acme.allowed-reports=ventas,stock,clientes
app.tenants.acme.allowed-formats=PDF,XLSX,DOCX,HTML       # optional; empty = all formats
```

Credentials use env-variable placeholders; `password` is never logged. `allowed-reports` is required per tenant — absent or empty blocks all reports (403).

## Error Handling

| Scenario | HTTP | Exception | Handler |
|----------|------|-----------|---------|
| Missing/unknown token | 401 | `TenantResolutionException` | `GlobalExceptionHandler` |
| Dedicated mode + wrong tenant | 403 | `TenantResolutionException` | `GlobalExceptionHandler` |
| Report not in allowlist | 403 | `ReportNotAllowedException` | `GlobalExceptionHandler` |
| Template missing in tenant dir | 404 | `ReportNotFoundException` (existing) | Existing handler |
| Datasource unreachable at runtime | 500 | `ReportGenerationException` (existing) | Existing handler |
| Startup: bad JDBC URL | Fail fast | Bean creation exception | Spring context shutdown |

Response body: plain-text error message (matches current pattern). Logging includes `tenantId` only — never credentials or connection strings.

## Testing Strategy

| Layer | Scope | Tools |
|-------|-------|-------|
| Unit | `ConfigBasedTenantResolver`: valid/invalid token → tenant/null | JUnit 5 + Mockito |
| Unit | `ConfigBasedAllowlistService`: allowed/disallowed/empty allowlist | JUnit 5 |
| Unit | `TenantScopedTemplateResolver`: path resolution, missing template | JUnit 5 |
| Unit | `LazyReportCompiler`: stale .jrxml triggers recompile, fresh .jasper skips | JUnit 5 |
| Unit | `DataSourceManager`: pool creation, validation, startup fail-fast | JUnit 5 + Mockito |
| Unit | `TokenValidator` + `TenantContextInitializer`: interceptor chain order, header parsing | JUnit 5 + MockMvc |
| Unit | `TenantContext` thread isolation: set → get within thread, null across threads, clear after request | JUnit 5 |
| Unit | `ReportOrchestrator` with mocked `ReportAllowlistService` (DIP): verify delegation, not implementation | JUnit 5 + Mockito |
| Unit | `JasperFiller` with mocked `TemplateResolver` + `ReportCompiler` (DIP) | JUnit 5 + Mockito |
| Unit | `QueryExecutor` with mocked `DataSourceProvider` (DIP) | JUnit 5 + Mockito |
| Integration | Full interceptor chain: valid/invalid token → expected HTTP status; dedicated mode rejection | `@SpringBootTest` with test profiles |
| Integration | End-to-end: token → tenant → allowlist → template → query → export → response | `@SpringBootTest` with H2 per tenant |

Interfaces make every unit test a pure mock test — no database, no filesystem, no Spring context needed. Integration tests validate the real wiring.

## Migration

No data migration. Existing single-tenant deployments migrate by setting `app.profile=dedicated` with one tenant definition using the current token → datasource → template path. Remove `service.token` and `app.reportes.ruta` properties.

## Open Questions

None — all design decisions, SOLID boundaries, and interface contracts are resolved.
