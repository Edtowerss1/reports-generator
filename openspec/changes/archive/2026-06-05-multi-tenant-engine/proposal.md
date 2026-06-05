# Proposal: Multi-Tenant Engine

## Intent

Replace the current single-tenant report engine with a stateless multi-tenant service. Today the engine assumes one shared auth token, one datasource, and one shared report directory, which blocks scalability.

## Scope

### In Scope
- Resolve tenant from `X-Service-Token` and map it to exactly one configured tenant.
- Route each request to the tenant datasource, template root, and allowed report catalog.
- Enforce report allowlists and support centralized or dedicated deployment modes.

### Out of Scope
- Dynamic tenant registration, admin UI, or self-service tenant management.
- JWT/OAuth2/API gateway integration.
- Schema-per-tenant, `tenant_id` filtering, or cross-tenant shared databases.

## Capabilities

### New Capabilities
- `multi-tenant-engine`: request-time tenant resolution, datasource routing, tenant template isolation, report allowlists, and deployment profiles.

### Modified Capabilities
- None.

## Approach

Keep the service stateless and introduce a deployment-controlled tenant registry. Each request validates the token, resolves the tenant, selects the correct datasource/template path, and verifies the report is allowed. Dedicated instances reuse the same contract.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ReportController` | Modified | Resolve tenant context from service token. |
| `ReportOrchestrator` | Modified | Carry tenant context through generation flow. |
| `JasperFiller` | Modified | Resolve tenant template root and parameters. |
| `QueryExecutor` | Modified | Execute against tenant-specific datasource. |
| `application.properties*` | Modified | Add tenant registry, tokens, catalogs, and datasource mappings. |
| `reportes/README.md` | Modified | Document tenant-specific report storage rules. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Tenant routing misconfiguration | Medium | Fail fast on startup and validate tenant entries. |
| Cross-tenant data leakage | High | Isolate datasource, template root, and report allowlist per tenant. |
| Token sprawl or weak secrets handling | Medium | Keep tokens config-only and plan JWT/API gateway evolution. |
| Operational drift between centralized and dedicated modes | Medium | Use one code path and only vary deployment config. |

## Rollback Plan

Revert to the current single-tenant deployment profile and shared datasource/template configuration. Keep the old token gate available as a fallback while tenant config is removed from deployment.

## Dependencies

- Static deployment configuration for tenant registry and datasource credentials.
- Tenant-owned database availability and report template storage.

## Success Criteria

- [ ] A configured token resolves to exactly one tenant at request time.
- [ ] Each tenant can only execute its configured allowlisted reports.
- [ ] Requests route to the correct tenant datasource and template root.
- [ ] The same codebase runs in centralized and dedicated deployment modes.
- [ ] No tenant can read or execute another tenant’s reports or datasource.
