# Design: Rename Project to JasperReport

## Technical Approach

Mechanical rename across the full repository — no behavior, API, or architecture changes.
The strategy is: **directories first** (preserving git history via `git mv`), then **file contents** (package declarations, imports, build/config identifiers), then **documentation**. A clean `./mvnw clean test` at the end confirms nothing broke.

> **Why this order**: Moving directories with `git mv` first ensures Git tracks the rename properly. If we edit file contents before `git mv`, Git sees the old path deleted and a new path created — losing history continuity.

## Architecture Decisions

This is a purely mechanical rename. There are **no architecture decisions**. The layer boundaries, strategy/registry patterns, constructor injection, and tenant resolution model remain unchanged.

| Consideration | What we do | Why |
|---|---|---|
| Git history | `git mv` for directory trees | Preserves file rename tracking; avoids delete+create semantics |
| Java refactor | sed-based batch replace of `com.example.JaspertReport` → `com.example.JasperReport` in all `.java` files under `src/` | Simple, fast, predictable; no IDE dependency. Each file changes exactly 1 line (`package` declaration) |
| Class renaming | `JaspertReportApplication.java` → `JasperReportApplication.java` | Matches class name to new package; mainClass must align |
| File content safety | No Java files reference `JaspertReport` as string literals (confirmed by exploration) | sed on `.java` files affects only package/import lines |
| Non-Java files | Hand-edit `pom.xml`, `.properties`, `.md`, and JSON | These contain mixed-case references requiring human judgment (artifactId vs DB name vs path segments) |

## Execution Order

### Phase 1: Directories (git mv)

```bash
git mv src/main/java/com/example/JaspertReport \
       src/main/java/com/example/JasperReport

git mv src/test/java/com/example/JaspertReport \
       src/test/java/com/example/JasperReport
```

This renames the package trees and automatically updates import paths where Git detects file renames. The class files themselves still contain old package declarations — fixed next.

> **Important**: Do NOT run `git mv` on the Postman collection file. Rename it with a separate commit to avoid coupling config changes with Java restructuring.

### Phase 2: Java File Contents (sed)

All 60 Java files (39 main + 21 test) need their `package` declaration updated. Since exploration confirmed zero string-literal occurrences of `JaspertReport` in Java code, a single sed pass is safe:

```bash
find src/main/java src/test/java -name "*.java" \
  -exec sed -i 's/^package com\.example\.JaspertReport;/package com.example.JasperReport;/' {} +
```

Rename the application class file:
```bash
git mv src/main/java/com/example/JasperReport/JaspertReportApplication.java \
       src/main/java/com/example/JasperReport/JasperReportApplication.java
```

And its test counterpart:
```bash
git mv src/test/java/com/example/JasperReport/JaspertReportApplicationTests.java \
       src/test/java/com/example/JasperReport/JasperReportApplicationTests.java
```

### Phase 3: Build and Config

Edit these files inline with string replacement:

| File | Change |
|---|---|
| `pom.xml` L13 | `<artifactId>JaspertReport</artifactId>` → `JasperReport` |
| `pom.xml` L15 | `<name>JaspertReport</name>` → `JasperReport` |
| `pom.xml` L134 | `<mainClass>com.example.JaspertReport.JaspertReportApplication</mainClass>` → `com.example.JasperReport.JasperReportApplication` |
| `application.properties` L1 | `spring.application.name=JaspertReport` → `JasperReport` |
| `application.properties` L15 | DB URL: `jaspertreport` → `jasperreport` |
| `application.properties` L34 | logging level: `com.example.JaspertReport` → `com.example.JasperReport` |
| `application-test.properties` L40 | logging level: `com.example.JaspertReport` → `com.example.JasperReport` |
| `test/resources/application-test.properties` L43 | logging level: `com.example.JaspertReport` → `com.example.JasperReport` |

### Phase 4: Documentation and Postman

Rename Postman file:
```bash
git mv postman/JaspertReport-MultiTenant.postman_collection.json \
       postman/JasperReport-MultiTenant.postman_collection.json
```

Edit 6 markdown files (`README.md`, `DEPLOYMENT.md`, `DEVELOPMENT.md`, `CONTRIBUTING.md`, `PRODUCTION.md`, `src/main/resources/reportes/README.md`):
- Replace `JaspertReport` → `JasperReport` everywhere
- Replace `/opt/jaspertreport/` → `/opt/jasperreport/`
- Replace `jaspertreport_dev` → `jasperreport_dev`
- Replace `jaspertreport.log` → `jasperreport.log`

Update Postman JSON: replace `JaspertReport` → `JasperReport` in `name`, `description`, and any request URLs.

### Phase 5: Verification

```bash
./mvnw clean test
```

Assert:
1. Compilation succeeds with zero errors
2. All 103 tests pass (unit + integration + E2E across 5 tenants, 19 spec scenarios)
3. `rg "com\.example\.JaspertReport" src/` returns **zero matches**
4. `rg "JaspertReport" --include "*.xml" --include "*.properties" --include "*.md" --include "*.json" | grep -v openspec/` returns zero matches outside historical artifacts

### Phase 6: Clean Residuals

```bash
./mvnw clean
```

This removes `target/` directories that may contain compiled classes with the old name.

## File Changes Summary

| Category | Action | Count |
|---|---|---|
| Directory rename (git mv) | Rename | 2 trees |
| Java source files (package declaration) | Modify | 39 |
| Java test files (package declaration) | Modify | 21 |
| Application class file | Rename + class edit | 1 |
| Application test class file | Rename | 1 |
| `pom.xml` | Modify | 1 (3 lines) |
| `.properties` files | Modify | 3 (6 lines total) |
| Markdown docs | Modify | 6 |
| Postman collection | Rename + edit | 1 |

## Rollback

```bash
git revert <rename-commit>
./mvnw clean test  # confirm old name restored
```

## Open Questions

None. This is a mechanical rename with unambiguous scope confirmed by exploration.
