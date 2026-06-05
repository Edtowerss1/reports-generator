# multi-tenant-engine Specification

## Purpose

Defines stateless multi-tenant report generation. Each request maps a service token to a tenant, scoping datasource, template root, and report allowlist. One codebase supports centralized (many tenants) and dedicated (one tenant) deployment modes.

## Requirements

### Requirement: Token-Based Tenant Resolution

The system MUST validate `X-Service-Token` on every request. Valid tokens MUST resolve to exactly one tenant from deployment config. Unknown, missing, or invalid tokens MUST return HTTP 401. The resolved tenant identity MUST be available throughout the request lifecycle.

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| R1 | Valid token resolves | `tok-A` → tenant `acme` in config | `X-Service-Token: tok-A` | Tenant `acme` resolved; request proceeds |
| R2 | Unknown token rejected | No tenant maps `tok-X` | `X-Service-Token: tok-X` | HTTP 401 |
| R3 | Missing token rejected | Header absent | No `X-Service-Token` header | HTTP 401 |
| R4 | One-to-one mapping | `tok-A` configured for `acme` only | Token resolution | Exactly one tenant returned |

### Requirement: Per-Tenant Datasource Routing

The system MUST select a tenant-specific datasource at request time (URL, credentials, driver per tenant). It MUST fail fast at startup if a configured datasource is unreachable. Tenant queries MUST NOT execute against another tenant's database.

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| D1 | Tenant query isolated | Tenant `acme` → DB `acme_db` | SQL executes | Query runs on `acme_db` only |
| D2 | Startup fails on bad config | Tenant `acme` has invalid JDBC URL | Application starts | Startup fails with clear error |
| D3 | Cross-tenant isolation | Tenants `acme`, `corp` configured | `acme` request processes | No query reaches `corp` DB |

### Requirement: Tenant-Scoped Template Resolution

The system MUST load `.jasper` templates from a tenant-specific root directory. `SUBREPORT_DIR` MUST resolve to the tenant's root. A missing template MUST produce an error — no fallback to a shared directory.

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| T1 | Template from tenant dir | Tenant `acme` root `/reports/acme/` | `reportName: ventas` | Loads `/reports/acme/ventas.jasper` |
| T2 | SUBREPORT_DIR scoped | Tenant `acme`; subreport in template | Fill executes | SUBREPORT_DIR = `acme` root |
| T3 | Template not found | No `missing.jasper` in tenant dir | `reportName: missing` | Error; no shared-folder fallback |

### Requirement: Report Allowlist Enforcement

The system MUST enforce a per-tenant allowlist of permitted report names. Requests for disallowed reports MUST return HTTP 403. The allowlist SHALL be static, deployment-configuration controlled.

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| A1 | Allowed report proceeds | Allowlist: `[ventas, stock]` | `reportName: ventas` | Generation begins |
| A2 | Disallowed report rejected | Allowlist: `[ventas]` | `reportName: nomina` | HTTP 403 |
| A3 | Empty allowlist blocks all | Allowlist: `[]` | Any report request | HTTP 403 |

### Requirement: Deployment Profiles

The system MUST support `centralized` (all tenants in one instance) and `dedicated` (one tenant per instance) profiles via config. A dedicated instance MUST reject tokens for other tenants. The same binary MUST operate correctly in both profiles.

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| P1 | Centralized multi-tenant | Profile `centralized`; tenants `acme`, `corp` | Requests for both tenants | Both served correctly |
| P2 | Dedicated single-tenant | Profile `dedicated`; assigned `acme` | Request with token for `corp` | HTTP 403 |
| P3 | Same binary, both modes | Same JAR; different config per instance | Deploy centralized + dedicated | Both operate independently |

### Requirement: End-to-End Request Flow

The system MUST execute: token→tenant→allowlist→datasource→template→query→fill→export→response. Any step failure MUST short-circuit with an appropriate HTTP error. Tenant isolation MUST hold across the entire flow.

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| F1 | Full happy path | Tenant `acme`: valid DB, templates, allowlist | Valid token + allowed report + valid format | Report bytes returned; correct content type |
| F2 | Mid-flow failure short-circuits | Tenant resolved; DB unreachable | Query execution step | HTTP 500; no partial content |
| F3 | Cross-tenant data isolation | Tenants `acme`, `corp` share no data | Full `acme` flow | Response contains only `acme` data |
