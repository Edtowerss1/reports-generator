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

- [ ] 1.1 Create `tenant/Tenant.java` — value record with id, reportesRuta, allowedReports, allowedFormats, datasource
- [ ] 1.2 Create `tenant/TenantContext.java` — ThreadLocal holder
- [ ] 1.3 Create `exceptions/TenantResolutionException.java` → 401
- [ ] 1.4 Create `exceptions/ReportNotAllowedException.java` → 403
- [ ] 1.5 Create `config/TenantProperties.java` — `@ConfigurationProperties("app")`
- [ ] 1.6 Unit tests: TenantContext isolation, TenantProperties binding
- [ ] 1.7 Update `application.properties` — replace single-tenant props with `app.tenants.*`
- [ ] 1.8 Update `application.properties.example`

## Phase 2: Core Abstractions — Interfaces + Implementations

- [ ] 2.1 Test + `TenantResolver` interface + `ConfigBasedTenantResolver`
- [ ] 2.2 Test + `ReportAllowlistService` interface + `ConfigBasedAllowlistService`
- [ ] 2.3 Test + `TemplateResolver` interface + `TenantScopedTemplateResolver`
- [ ] 2.4 Test + `ReportCompiler` interface + `LazyReportCompiler`
- [ ] 2.5 Test + `DataSourceProvider` interface + `DataSourceManager` (HikariCP per tenant)

## Phase 3: Wiring — Interceptors, Config, Error Handlers

- [ ] 3.1 Create `TokenValidator` — validates header, delegates to TenantResolver
- [ ] 3.2 Create `TenantContextInitializer` — resolves tenant, enforces dedicated mode, populates TenantContext
- [ ] 3.3 Create `config/WebConfig.java` — register interceptors on `/reportes/**`
- [ ] 3.4 Modify `GlobalExceptionHandler` — add handlers for 401/403
- [ ] 3.5 Unit tests: TokenValidator, TenantContextInitializer (dedicated mode)
- [ ] 3.6 Integration test: MockMvc interceptor chain for valid/invalid/dedicated-rejection

## Phase 4: Service Integration — Wire Interfaces into Existing Services

- [ ] 4.1 Modify `ReportOrchestrator` — inject `ReportAllowlistService`, enforce before fill
- [ ] 4.2 Unit test: Orchestrator with mocked AllowlistService
- [ ] 4.3 Modify `JasperFiller` — inject `TemplateResolver`+`ReportCompiler`, remove @PostConstruct compile
- [ ] 4.4 Unit test: JasperFiller with mocked resolver+compiler
- [ ] 4.5 Modify `QueryExecutor` — inject `DataSourceProvider` instead of JdbcTemplate
- [ ] 4.6 Unit test: QueryExecutor with mocked DataSourceProvider
- [ ] 4.7 Modify `ReportController` — remove inline token check (handled by interceptors)

## Phase 5: Integration Testing — End-to-End Flow

- [ ] 5.1 E2E happy path: valid token + allowed report → bytes (R1, A1, D1, T1, F1)
- [ ] 5.2 Error flows: unknown token 401 (R2), missing token 401 (R3), disallowed 403 (A2, A3)
- [ ] 5.3 Dedicated mode: wrong tenant token → 403 (P2, P3)
- [ ] 5.4 Failure isolation: mid-flow fail short-circuits (F2), cross-tenant isolation (D3, F3)
