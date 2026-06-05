# Tasks: Multi-Tenant Engine

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1,100–1,300 |
| 400-line budget risk | **High** |
| Chained PRs recommended | **Yes** |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: **Yes**
Chained PRs recommended: **Yes**
Chain strategy: pending
400-line budget risk: **High**

### Suggested Work Units

| Unit | Goal | Est. Lines |
|------|------|------------|
| 1 | Foundation: Tenant, TenantContext, exceptions, TenantProperties, config | ~300 |
| 2 | Core: 5 interfaces + 5 implementations + unit tests | ~450 |
| 3 | Wiring: interceptors, WebConfig, service mods, error handlers + tests | ~350 |
| 4 | Integration: E2E tests for all 6 requirements (R1–F3) | ~200 |

## Phase 1: Foundation — Model, Context, Exceptions, Config

- [x] 1.1 Create `tenant/Tenant.java` — value record with id, reportesRuta, allowedReports, allowedFormats, datasource
- [x] 1.2 Create `tenant/TenantContext.java` — ThreadLocal holder
- [x] 1.3 Create `exceptions/TenantResolutionException.java` → 401
- [x] 1.4 Create `exceptions/ReportNotAllowedException.java` → 403
- [x] 1.5 Create `config/TenantProperties.java` — `@ConfigurationProperties("app")`
- [x] 1.6 Unit tests: TenantContext isolation, TenantProperties binding
- [x] 1.7 Update `application.properties` — added `app.profile`, `app.assigned-tenant` alongside existing single-tenant props (full removal in Phase 4)
- [x] 1.8 Update `application.properties.example`

## Phase 2: Core Abstractions — Interfaces + Implementations

- [x] 2.1 Test + `TenantResolver` interface + `ConfigBasedTenantResolver`
- [x] 2.2 Test + `ReportAllowlistService` interface + `ConfigBasedAllowlistService`
- [x] 2.3 Test + `TemplateResolver` interface + `TenantScopedTemplateResolver`
- [x] 2.4 Test + `ReportCompiler` interface + `LazyReportCompiler`
- [x] 2.5 Test + `DataSourceProvider` interface + `DataSourceManager` (HikariCP per tenant)

## Phase 3: Wiring — Interceptors, Config, Error Handlers

- [x] 3.1 Create `TokenValidator` — validates header, delegates to TenantResolver
- [x] 3.2 Create `TenantContextInitializer` — resolves tenant, enforces dedicated mode, populates TenantContext
- [x] 3.3 Create `config/WebConfig.java` — register interceptors on `/reportes/**`
- [x] 3.4 Modify `GlobalExceptionHandler` — add handlers for 401/403 *(completed in Phase 1/2: commit 594f88c)*
- [x] 3.5 Unit tests: TokenValidator, TenantContextInitializer (dedicated mode)
- [x] 3.6 Integration test: MockMvc interceptor chain for valid/invalid/dedicated-rejection

## Phase 4: Service Integration — Wire Interfaces into Existing Services

- [x] 4.1 Modify `ReportOrchestrator` — inject `ReportAllowlistService`, enforce before fill
- [x] 4.2 Unit test: Orchestrator with mocked AllowlistService
- [x] 4.3 Modify `JasperFiller` — inject `TemplateResolver`+`ReportCompiler`, remove @PostConstruct compile
- [x] 4.4 Unit test: JasperFiller with mocked resolver+compiler
- [x] 4.5 Modify `QueryExecutor` — inject `DataSourceProvider` instead of JdbcTemplate
- [x] 4.6 Unit test: QueryExecutor with mocked DataSourceProvider
- [x] 4.7 Modify `ReportController` — remove inline token check (handled by interceptors)

## Phase 5: Integration Testing — End-to-End Flow

- [x] 5.1 E2E happy path: valid token + allowed report → bytes (R1, A1, D1, T1, F1)
- [x] 5.2 Error flows: unknown token 401 (R2), missing token 401 (R3), disallowed 403 (A2, A3)
- [x] 5.3 Dedicated mode: wrong tenant token → 403 (P2, P3)
- [x] 5.4 Failure isolation: mid-flow fail short-circuits (F2), cross-tenant isolation (D3, F3)
