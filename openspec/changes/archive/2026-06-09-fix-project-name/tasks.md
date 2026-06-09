# Tasks: Rename Project to JasperReport

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~180 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | `ask-on-risk` |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

**Why**: 60 of ~180 lines are single-line package declaration changes in Java files. The rest is config/docs. This is a mechanical rename with zero behavioral risk — well under the 400-line budget.

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | All rename work (phases 1–6) | Single PR | Autonomous, self-contained, verifiable with `./mvnw clean test` |

---

## Phase 1: Directories — `git mv`

- [x] 1.1 `git mv src/main/java/com/example/JaspertReport/ src/main/java/com/example/JasperReport/`
- [x] 1.2 `git mv src/test/java/com/example/JaspertReport/ src/test/java/com/example/JasperReport/`

## Phase 2: Java File Contents

- [x] 2.1 Run `sed` on all `.java` files under both `src/` trees: replace ALL `com.example.JaspertReport` → `com.example.JasperReport` (156 occurrences, not just package lines)
- [x] 2.2 Rename `JaspertReportApplication.java` → `JasperReportApplication.java` + update class declaration
- [x] 2.3 Rename `JaspertReportApplicationTests.java` → `JasperReportApplicationTests.java`

## Phase 3: Build and Config

- [x] 3.1 Update `pom.xml`: `<artifactId>`, `<name>`, `<mainClass>` (3 changes)
- [x] 3.2 Update `src/main/resources/application.properties`: `spring.application.name`, DB URL `jasperreport`, logging level (3 changes)
- [x] 3.3 Update `src/main/resources/application-test.properties`: logging level (1 change)
- [x] 3.4 Update `src/test/resources/application-test.properties`: logging level (1 change)

## Phase 4: Documentation and Postman

- [x] 4.1 Update 6 markdown docs (`README.md`, `DEPLOYMENT.md`, `DEVELOPMENT.md`, `CONTRIBUTING.md`, `PRODUCTION.md`, `src/main/resources/reportes/README.md`): rename `JaspertReport` → `JasperReport`, path/DB/log references
- [x] 4.2 `git mv` Postman file + update JSON `name`, `description`, and request URLs

## Phase 5: Verification

- [x] 5.1 Run `./mvnw clean test` — BUILD SUCCESS, 104 tests pass (0 failures, 0 errors, 0 skipped)
- [x] 5.2 Run `rg "com\.example\.JaspertReport" src/` — zero matches ✅
- [x] 5.3 Run `rg "JaspertReport" --glob '!target/**' --glob '!openspec/**'` — zero matches ✅

## Phase 6: Clean Residuals

- [x] 6.1 Run `./mvnw clean` to purge stale `target/` artifacts with old package name

---

### Notes

- **No commits during apply**: The user will review all changes, then commit everything once functional.
- **Order matters**: Phase 1 must precede Phase 2 (directories first so `git mv` preserves history). Phase 5 & 6 are verification and cleanup — run last.
- **Spec coverage**: Each requirement maps to one or more tasks. Verification tasks (5.1–5.3) assert all spec scenarios pass.
